package com.mikivan.service;


        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.util.concurrent.Executors;

        import javax.xml.transform.OutputKeys;
        import javax.xml.transform.Templates;
        import javax.xml.transform.TransformerFactory;
        import javax.xml.transform.sax.SAXTransformerFactory;
        import javax.xml.transform.sax.TransformerHandler;
        import javax.xml.transform.stream.StreamResult;
        import javax.xml.transform.stream.StreamSource;

        import org.dcm4che3.data.*;
        import org.dcm4che3.io.DicomOutputStream;
        import org.dcm4che3.io.SAXWriter;
        import org.dcm4che3.net.ApplicationEntity;
        import org.dcm4che3.net.Association;
        import org.dcm4che3.net.Connection;
        import org.dcm4che3.net.Device;
        import org.dcm4che3.net.DimseRSPHandler;
        import org.dcm4che3.net.Status;
        import org.dcm4che3.net.pdu.AAssociateRQ;
        import org.dcm4che3.net.pdu.PresentationContext;
        import org.dcm4che3.tool.common.CLIUtils;
        import org.dcm4che3.util.SafeClose;
        import org.dcm4che3.util.TagUtils;


public class CFindSCU {

    // Context Presentation UID
    private static String cuid = UID.StudyRootQueryRetrieveInformationModelFIND;

    // Transfer Syntax
    private static String[] IVR_LE_FIRST = new String[]{"1.2.840.10008.1.2", "1.2.840.10008.1.2.1", "1.2.840.10008.1.2.2"};
    private static String[] EVR_LE_FIRST = new String[]{"1.2.840.10008.1.2.1", "1.2.840.10008.1.2.2", "1.2.840.10008.1.2"};
    private static String[] EVR_BE_FIRST = new String[]{"1.2.840.10008.1.2.2", "1.2.840.10008.1.2.1", "1.2.840.10008.1.2"};
    private static String[] IVR_LE_ONLY  = new String[]{"1.2.840.10008.1.2"};


    private final Device device = new Device("findscu");
    private final ApplicationEntity ae = new ApplicationEntity("findscu");
    private final Connection conn = new Connection();
    private final Connection remote = new Connection();
    private final AAssociateRQ rq = new AAssociateRQ();

    private Attributes keys = new Attributes();

    private int priority;
    private int cancelAfter;

    private static SAXTransformerFactory saxtf;

    private boolean xml = true;                  //"-X", "--xml" - вывод будет xml или обработаннный xslt.
    private File xsltFile;
    private OutputStream out;

    private boolean isConstructorWithArgs = false;


    private class DimseRSPHandlerCFineSCU extends DimseRSPHandler {

        private DimseRSPHandlerCFineSCU(int msgId) {
            super(msgId);
        }

        int cancelAfter = CFindSCU.this.cancelAfter;
        int numMatches;

        @Override
        public void onDimseRSP( Association as, Attributes cmd, Attributes data) {

            System.out.println("numMatches+++++++++++++++++++++++++++++++++++++++++   " + numMatches);
            System.out.println("cancelAfter++++++++++++++++++++++++++++++++++++++++   " + cancelAfter);

            super.onDimseRSP(as, cmd, data);
            int status = cmd.getInt(Tag.Status, -1);
            if (Status.isPending(status)) {

                CFindSCU.this.printAttributes(cmd);
                System.out.println("int status = cmd.getInt(Tag.Status, -1) = " + status);
                CFindSCU.this.printAttributes(data);

                CFindSCU.this.onResult(data);

                ++numMatches;
                if (cancelAfter != 0 && numMatches >= cancelAfter)
                    try {
                        System.out.println("-- cancelAfter -- cancelAfter -- cancelAfter -- cancelAfter -- cancelAfter --");
                        cancel(as);
                        cancelAfter = 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }



    public final void setPriority(int priority) { this.priority = priority;}

    public final void setCancelAfter(int cancelAfter) {
        this.cancelAfter = cancelAfter;
    }

    public final void setXSLT(File xsltFile) {
        this.xsltFile = xsltFile;
    }

    public final void setXML(boolean xml) { this.xml = xml; }


    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
     public CFindSCU( String[] b,
                      String[] c,
                      String[] opts,
                      String   fileXSLT,
                      String   findLevel,
                      String[] m,
                      String[] r) throws IOException {

        // local service connector
        this.conn.setHostname(b[1]);
        this.conn.setPort(Integer.parseInt(b[2]));

        // remote pacs
        this.remote.setHostname(c[1]);
        this.remote.setPort(Integer.parseInt(c[2]));
        //this.remote.setTlsProtocols(this.conn.getTlsProtocols());
        //this.remote.setTlsCipherSuites(this.conn.getTlsCipherSuites());

        configure(this.conn, opts); //замена CLIUtils.configure(this.conn, cl); //настройки proxy, есть в CLIUtils.configureConnect

        this.ae.setAETitle(b[0]);
        this.ae.addConnection(conn);

        this.rq.setCalledAET(c[0]);
        this.rq.setCallingAET(b[0]);
        this.rq.addPresentationContext(new PresentationContext(1, cuid, IVR_LE_FIRST));

        // составляем аттрибуты для отправке в запросе
        keys.setString(Tag.QueryRetrieveLevel, VR.CS, findLevel);
        CLIUtils.addEmptyAttributes(this.keys, r);
        CLIUtils.addAttributes(this.keys, m);


        if (!fileXSLT.equals("")) this.setXSLT( new File( fileXSLT ) );

        //this.setPriority(CLIUtils.priorityOf(cl));
        this.setPriority(0);

        // через сколько направленных с удаленного пакса записей отменить ассоциацию, ноль не отменять, принять все найденные записи
        this.setCancelAfter(0);

        this.isConstructorWithArgs = true;
    }


    public String doFind() throws  Exception{
        if(this.isConstructorWithArgs){

            this.device.addConnection(conn);
            this.device.addApplicationEntity(ae);

            this.device.setExecutor2( Executors.newSingleThreadExecutor() );
            this.device.setScheduledExecutor( Executors.newSingleThreadScheduledExecutor() );

            Association as = ae.connect(conn, remote, rq);

            DimseRSPHandler rspHandler = new DimseRSPHandlerCFineSCU( as.nextMessageID() );
            as.cfind( cuid, priority, this.keys, null, rspHandler);

            if (as.isReadyForDataTransfer()) {

                as.waitForOutstandingRSP();  // обработка получаемых данных в onDimseRSP из rspHandler
                as.release();
            }

            //вывод
            String output;
            if( this.out == null )
                 output = null;
            else output = this.out.toString();

            SafeClose.close(this.out);
            this.out = null;
            //

            this.device.getExecutor2().shutdown();
            this.device.getScheduledExecutor().shutdown();

            as.waitForSocketClose();

            this.isConstructorWithArgs = false;

            return output;
        }
        else{

            return null;
        }
    }
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------


    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
//======================================================================================================================
//-b IVAN@192.168.0.74:4006 -c PACS01@192.168.0.35:4006 -L STUDY -m StudyDate=20120101-20161231 -m ModalitiesInStudy=CT --out-cat  -X -K -I
        try{

//            String[] bind   = { "IVAN",    "192.168.121.101", "4006"};//строгий порядок
//            String[] remote = { "WATCHER", "192.168.121.100", "4006"};//строгий порядок
            String[] bind   = { "IVAN",   "192.168.0.74", "4006"};//строгий порядок
            String[] remote = { "PACS01", "192.168.0.55", "4006"};//строгий порядок
            String[] opts   = {};
            String[] m      = { "StudyDate", "20111004-20171004", "ModalitiesInStudy", "CT"};
            String[] r      = {"0020000D", "00080020", "00080030", "00080050", "00080090", "00100010", "00100020",
                               "00100030", "00100040", "00200010", "00201206", "00201208", "00081030", "00080060",
                               "00080061"};


            CFindSCU main = new CFindSCU(bind, remote, opts, "xslt/mikivan-studies.xsl","STUDY", m, r);

            String xsltOutput = main.doFind();


            if( xsltOutput == null )
                 System.out.println("xsltOutput == null");
            else System.out.println(xsltOutput);


        } catch (Exception e) {
            System.err.println("c-find-scu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
//======================================================================================================================

    private static void configure(Connection conn, String[] opts) throws IOException {
        //пока сделаем все по умолчанию, предполагая список опций в - opts
        //каждый ключ на своем строгом месте в массиве т.е. строгий порядок
        conn.setReceivePDULength(16378);  //"max-pdulen-rcv"
        conn.setSendPDULength(16378);     //"max-pdulen-snd"

        //используем не асинхронный режим if (cl.hasOption("not-async")) {
        conn.setMaxOpsInvoked(1);         //"max-ops-invoked"
        conn.setMaxOpsPerformed(1);       //"max-ops-performed"
//      } else {
//          conn.setMaxOpsInvoked(getIntOption(cl, "max-ops-invoked", 0));
//          conn.setMaxOpsPerformed(getIntOption(cl, "max-ops-performed", 0));
//      }

        conn.setPackPDV(false);           //"not-pack-pdv"
        conn.setConnectTimeout(0);        //"connect-timeout"
        conn.setRequestTimeout(0);        //"request-timeout"
        conn.setAcceptTimeout(0);         //"accept-timeout"
        conn.setReleaseTimeout(0);        //"release-timeout"
        conn.setResponseTimeout(0);       //"response-timeout"
        conn.setRetrieveTimeout(0);       //"retrieve-timeout"
        conn.setIdleTimeout(0);           //"idle-timeout"
        conn.setSocketCloseDelay(50);     //"soclose-delay"
        conn.setSendBufferSize(0);        //"sosnd-buffer"
        conn.setReceiveBufferSize(0);     //"sorcv-buffer"
        conn.setTcpNoDelay(false);        //"tcp-delay"

        // пока без применения TLS протокола
        //configureTLS(conn, cl);
    }


    private void onResult(Attributes data) {

        try {
            if (out == null) {

                out = new ByteArrayOutputStream();
            }
            if (xml) {
                writeAsXML(data, out);
            } else {
                DicomOutputStream dos = new DicomOutputStream(out, UID.ImplicitVRLittleEndian);
                dos.writeDataset(null, data);
            }
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            SafeClose.close(out);
            out = null;
        }
//        finally {
//            if (!catOut) {      // catOut = true; "--out-cat" - объеденить все ответы от удаленного диком сервера
//                SafeClose.close(out);
//                out = null;
//            }
//        }
    }


    private void writeAsXML(Attributes attrs, OutputStream out) throws Exception {
        TransformerHandler th = getTransformerHandler();
        th.getTransformer().setOutputProperty(OutputKeys.INDENT,"yes");   //private boolean xmlIndent = true;
        th.setResult(new StreamResult(out));
        SAXWriter saxWriter = new SAXWriter(th);
        saxWriter.setIncludeKeyword(false);  //"-K", "--no-keyword" - включать ли keyword в xml вывод
        saxWriter.setIncludeNamespaceDeclaration(false);
        saxWriter.write(attrs);
    }

    private TransformerHandler getTransformerHandler() throws Exception {
        SAXTransformerFactory tf = saxtf;
        if (tf == null)
            saxtf = tf = (SAXTransformerFactory) TransformerFactory
                    .newInstance();
        if (xsltFile == null)
            return tf.newTransformerHandler();

        Templates xsltTpls = tf.newTemplates(new StreamSource(xsltFile));

        return tf.newTransformerHandler(xsltTpls);
    }



    private void printAttributes( Attributes instanceAttr ){

        int nAttr = instanceAttr.size();

        for(int i = 0; i < nAttr; i++){

            int Tag_i = instanceAttr.tags()[i];
            String Tag_VR = instanceAttr.getVR(Tag_i).toString();

            System.out.print(
                    "[Attribute]/> " + "[" + nAttr + ", " + i + "] -> " +
                    TagUtils.toHexString(Tag_i) + " :->  " + TagUtils.toString(Tag_i) + "  :  " + Tag_VR + "  >  "
            );

            if( Tag_VR.equals("DA")|| Tag_VR.equals("PN")|| Tag_VR.equals("UI")|| Tag_VR.equals("CS")|| Tag_VR.equals("TM") ){
                System.out.println(instanceAttr.getString( Tag_i ));
            }else{
                System.out.println();
            }
        }
    }


}
