package com.mikivan.OHIFGateway;


//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.Map;
import org.dcm4che3.tool.findscu.FindSCU;



@Controller
@EnableAutoConfiguration
public class GateController {

    //private static final Logger log = LoggerFactory.getLogger(GateController.class);

    @RequestMapping( value = "/mikivan/studies", method = RequestMethod.GET )
    @ResponseBody
    public ResponseEntity<String> getStudylist(
            @RequestParam Map<String, String> queryMap ) {


        ArrayList<String> opts = new ArrayList<String>();

        opts.add("-b"); opts.add("IVAN@192.168.0.74:4006");
        opts.add("-c"); opts.add("PACS01@192.168.0.35:4006");
        opts.add("-L"); opts.add("STUDY");


//onCoreViewer/Packages/ohif-study-list/server/services/qido/studies.js:20
//PatientName
//PatientID
//AccessionNumber
//StudyDescription
//ModalitiesInStudy
//limit         игнорируем (а многоли включать данных в ответ)
//includefield  игнорируем (какие еще поля  вернуть, мы вернем необходимый минимум)
//StudyDate
        for( Map.Entry<String, String> entry : queryMap.entrySet() ){

            if(  (!entry.getKey().equals("limit")) &&
                    (!entry.getKey().equals("includefield"))
                    ) {

                opts.add("-m");opts.add(entry.getKey() + "=" + entry.getValue());

                System.out.println("-m " + entry.getKey() + "=" + entry.getValue());
            }
        }

//http://192.168.0.74:8080/mikivan/studies?PatientName=Mikh*&ModalitiesInStudy=CT&includefield=00081030%2C00080060
        //параметры взяты из метеора
        String[] r = {"0020000D", "00080020", "00080030", "00080050", "00080090", "00100010", "00100020", "00100030",
                      "00100040", "00200010", "00201206", "00201208", "00081030", "00080060", "00080061"};

        for(String teg:r) {
            opts.add("-r");opts.add(teg);
        }

        opts.add("--out-cat");

        //либо либо либо
        //opts.add("-X");
        //opts.add("--xsl");opts.add("xslt/oncore-json_compact.xsl");
        opts.add("--xsl");opts.add("xslt/mikivan-studies.xsl");

        opts.add("-K");
        opts.add("-I");


        String toViewStudyList =  "";

        try {

            String[] args = new String[opts.size()];

            opts.toArray(args);

            FindSCU main = new FindSCU(args);

            String jsonOutput = main.doFind();

            if( jsonOutput == null ){
                System.out.println("jsonOutput is null");
            }
            else {
                System.out.println("jsonOutput is created");

                toViewStudyList =  toViewStudyList + "[" + jsonOutput + "{}]";
            }
        } catch (Exception e) {
            System.err.println("findscu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }


        //ответ на запрос /mikivan/studylist
        HttpHeaders hdrs = new HttpHeaders();
        final MediaType mediaType = MediaType.APPLICATION_JSON;
        hdrs.setContentType(mediaType);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(toViewStudyList, hdrs, HttpStatus.OK);

        return rsp;
    }


//======================================================================================================================
//======================================================================================================================
//======================================================================================================================
    @RequestMapping( value = "/mikivan/studies/{studyUID}/metadata", method = RequestMethod.GET )
    @ResponseBody
    public ResponseEntity<String> getStudyMedadata( @PathVariable String studyUID ) {

        //http://192.168.0.74:8080/mikivan/studies/1.2.840.113704.1.111.4156.1367430813.2/metadata

        //log.info("/mikivan/studies/{studyUID}/metadata");
        System.out.println("/mikivan/studies/{studyUID}/metadata = " + "/mikivan/studies/" + studyUID + "/metadata");

        ArrayList<String> test = new ArrayList<String>();

        test.add("-b"); test.add("IVAN@192.168.0.74:4006");
        test.add("-c"); test.add("PACS01@192.168.0.35:4006");
        test.add("-L"); test.add("IMAGE");
        test.add("-m");test.add("0020000D=" + studyUID);

//        test.add("-r");test.add("00080020");
//        test.add("-r");test.add("00080030");
//        test.add("-r");test.add("00080060");
//        test.add("-r");test.add("00080061");
////         test.add("-r");test.add("00080090");
////         test.add("-r");test.add("00081030");
//        test.add("-r");test.add("00081190");
//        test.add("-r");test.add("00100010");
//        test.add("-r");test.add("00100020");
//        test.add("-r");test.add("00100030");
//        test.add("-r");test.add("00100040");
//        test.add("-r");test.add("0020000D");
//        test.add("-r");test.add("00200010");
//        test.add("-r");test.add("00201206");
//        test.add("-r");test.add("00201208");

        test.add("--out-cat");
        test.add("--xsl");test.add("xslt/oncore-json_compact.xsl");
        test.add("-K");
        test.add("-I");


        String val2 =  "";

        //[injections of mikivan][0004]
        try {

            String[] args = new String[test.size()];

            test.toArray(args);

/*             String[] args = {
                     "-b", "IVAN@192.168.0.74:4006",
                     "-c", "PACS01@192.168.0.35:4006",
                     "-L", "STUDY",
                     "-m", "StudyDate=20120101-20161231",
//                     "-m", "ModalitiesInStudy=CT",
                     "-m", "PatientSex=M",
//                     "-r", "00080005",
                     "-r", "00080020",
                     "-r", "00080030",
//                     "-r", "00080050",
                     "-r", "00080060",
                     "-r", "00080061",
//                     "-r", "00080090",
//                     "-r", "00081030",
//                     "-r", "00081190",
                     "-r", "00100010",
                     "-r", "00100020",
                     "-r", "00100030",
                     "-r", "00100040",
                     "-r", "0020000D",
                     "-r", "00200010",
                     "-r", "00201206",
                     "-r", "00201208",
                     "--out-cat",
                     "--xsl", "xslt/oncore-json_compact.xsl",//"-X",//
                     "-K",
                     "-I"
             };*/


            FindSCU main = new FindSCU(args);

            String jsonOutput = main.doFind();


            //вывод doFind();
            if( jsonOutput == null ){
                System.out.println("jsonOutput == null");
            }
            else {
                System.out.println(jsonOutput);

                val2 =  val2 + "[" + jsonOutput + "{}]";
            }


        } catch (Exception e) {
            System.err.println("findscu: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
        //[end][0004]



        //готовим ответ на запрос /mikivan/studylist
        HttpHeaders hdrs = new HttpHeaders();
        final MediaType mediaType = MediaType.APPLICATION_JSON;
        hdrs.setContentType(mediaType);

        final ResponseEntity<String> rsp = new ResponseEntity<String>(val2, hdrs, HttpStatus.OK);

        return rsp;
    }




}
