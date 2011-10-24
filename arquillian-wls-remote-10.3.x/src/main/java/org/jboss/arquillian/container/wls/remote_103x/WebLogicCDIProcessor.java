package org.jboss.arquillian.container.wls.remote_103x;

import org.jboss.arquillian.container.test.spi.TestDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.ProtocolArchiveProcessor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * WebLogic 10.3.x (and prior versions) packages the contents of WEB-INF\classes
 * into a JAR file _wl_cls_gen.jar (placed in WEB-INF\lib) during deployment.
 * If beans.xml is present in the WEB-INF directory, then Weld will be unable to locate it,
 * since the classloader in WebLogic will attempt to find it in the _wl_cls_gen.jar
 * and other JARs in WEB-INF\lib.
 * 
 * This {@link ProtocolArchiveProcessor} will relocate the beans.xml found in WEB-INF
 * of all protocol deployments, to the WEB-INF/classes/META-INF directory of the archive.
 * When WebLogic packages the WEB-INF\classes contents into the _wl_cls_gen.jar,
 * the classloader will now find the beans.xml file, as it would be placed in the META-INF directory
 * of _wl_cls_gen.jar.
 * 
 * @author Vineet Reynolds
 *
 */
public class WebLogicCDIProcessor implements ProtocolArchiveProcessor
{

   public void process(TestDeployment testDeployment, Archive<?> protocolArchive)
   {
      Archive<?> testArchive = testDeployment.getApplicationArchive();
      relocateBeansXML(testArchive, protocolArchive);
   }

   private void relocateBeansXML(Archive<?> testArchive, Archive<?> protocolArchive)
   {
      if(WebArchive.class.isInstance(protocolArchive))
      {
         WebArchive webProtocolArchive = WebArchive.class.cast(protocolArchive);
         Asset beansXML = testArchive.delete("WEB-INF/beans.xml").getAsset();
         webProtocolArchive.addAsWebInfResource(beansXML,"classes/META-INF/beans.xml");
      }
      else if(EnterpriseArchive.class.isInstance(protocolArchive))
      {
         EnterpriseArchive enterpriseProtocolArchive = EnterpriseArchive.class.cast(protocolArchive);
         for(WebArchive nestedWebProtocolArchive : enterpriseProtocolArchive.getAsType(WebArchive.class, Filters.include("/.*\\.war")))
         {
            Asset beansXML = nestedWebProtocolArchive.delete("WEB-INF/beans.xml").getAsset();
            nestedWebProtocolArchive.addAsWebInfResource(beansXML,"classes/META-INF/beans.xml");
         }
      }
   }

}
