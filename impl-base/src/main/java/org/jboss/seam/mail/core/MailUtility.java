package org.jboss.seam.mail.core;

import java.net.UnknownHostException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;

import org.jboss.seam.mail.core.enumurations.RecipientType;
import org.jboss.seam.mail.util.Strings;
/**
 * 
 * @author Cody Lerum
 *
 */
public class MailUtility
{

   public static String getHostName()
   {
      try
      {
         java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
         return localMachine.getHostName();
      }
      catch (UnknownHostException e)
      {
         return "localhost";
      }
   }  

   public static Session buildMailSession(MailConfig mailConfig)
   {
      Session session;

      Properties props = new Properties();

      if (mailConfig.isValid())
      {
         props.put("mail.smtp.host", mailConfig.getServerHost());
         props.put("mail.smtp.port", mailConfig.getServerPort());
      }
      else
      {
         throw new RuntimeException("ServerHost and ServerPort must be set in MailConfig");
      }

      if (!Strings.isNullOrEmpty(mailConfig.getDomainName(), true))
      {
         props.put("mail.seam.domainName", mailConfig.getDomainName());
      }

      if (mailConfig.getUsername() != null && mailConfig.getUsername().length() != 0 && mailConfig.getPassword() != null && mailConfig.getPassword().length() != 0)
      {
         MailSessionAuthenticator authenticator = new MailSessionAuthenticator(mailConfig.getUsername(), mailConfig.getPassword());

         session = Session.getInstance(props, authenticator);
      }
      else
      {
         session = Session.getInstance(props, null);
      }
      return session;
   }

   public static String headerStripper(String header)
   {
      if (!Strings.isNullOrEmpty(header, true))
      {
         String s = header.trim();

         if (s.matches("^<.*>$"))
         {
            return header.substring(1, header.length() - 1);
         }
         else
         {
            return header;
         }
      }
      else
      {
         return header;
      }
   }

   public static void send(EmailMessage e, Session session)
   {
      BaseMailMessage b = new BaseMailMessage(session, e.getRootSubType());

      if (!Strings.isNullOrEmpty(e.getMessageId(), true))
      {
         b.setMessageID(e.getMessageId());
      }

      b.setFrom(e.getFromAddress());
      b.addRecipients(RecipientType.TO, e.getToAddresses());
      b.addRecipients(RecipientType.CC, e.getCcAddresses());
      b.addRecipients(RecipientType.BCC, e.getBccAddresses());
      b.setReplyTo(e.getReplyToAddresses());
      b.addDeliveryRecieptAddresses(e.getDeliveryReceiptAddresses());
      b.addReadRecieptAddresses(e.getReadReceiptAddresses());
      b.setImportance(e.getImportance());

      if (e.getSubject() != null)
      {
         b.setSubject(e.getSubject());
      }

      if (e.getHtmlBody() != null && e.getTextBody() != null)
      {
         b.setHTMLTextAlt(e.getHtmlBody(), e.getTextBody());
      }
      else if (e.getTextBody() != null)
      {
         b.setText(e.getTextBody());
      }
      else if (e.getHtmlBody() != null)
      {
         b.setHTML(e.getHtmlBody());
      }

      b.addAttachments(e.getAttachments());

      b.send();

      try
      {
         e.setMessageId(null);
         e.setLastMessageId(MailUtility.headerStripper(b.getFinalizedMessage().getMessageID()));
      }
      catch (MessagingException e1)
      {
         throw new RuntimeException("Unable to read Message-ID from sent message");
      }
   }
}
