package org.apache.lucene.demo;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.Analizador;
import org.apache.lucene.analysis.es.AnalizadorNombre;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.FSDirectory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {
    String usage =
            "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
    if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
      System.out.println(usage);
      System.exit(0);
    }
    String infoNeeds = "C:\\Users\\Portatil\\Desktop\\Davy\\7CUATRI\\RI\\recuperacion\\trabajo\\src\\selectedInformationNeeds.xml";
    String index = "index";
    String output = "resultados.txt";
    String queryString = null;
    int hitsPerPage = 10;

    for (int i = 0; i < args.length; i++) {
      if ("-index".equals(args[i])) {
        index = args[i + 1];
        i++;
      } else if ("-infoNeeds".equals(args[i])) {
        infoNeeds = args[i + 1];
        i++;
      } else if ("-output".equals(args[i])) {
        output = args[i + 1];
        i++;
      }
    }

    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new Analizador();
    Analyzer nameAnalyzer = new AnalizadorNombre();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    org.w3c.dom.Document document = builder.parse(new File(infoNeeds));

    File archivo = new File ("C:\\Users\\Portatil\\Desktop\\Davy\\7CUATRI\\RI\\recuperacion\\trabajo\\src\\nombres.txt");
    FileReader fr = new FileReader (archivo);
    BufferedReader br = new BufferedReader(fr);

    String linea;
    ArrayList<String> nombres = new ArrayList<String>();
    while((linea=br.readLine())!=null){
      nombres.add(linea);
    }

    Element e = document.getDocumentElement();
    NodeList nodes = e.getChildNodes();
    String informationNeeds[][] = new String[5][2];
    int indice = 0;
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i).getNodeName().equals("informationNeed")) {
        NodeList aux = nodes.item(i).getChildNodes();
        for (int j = 0; j < aux.getLength(); j++) {
          if (aux.item(j).getNodeName().equals("identifier")) {
            informationNeeds[indice][0] = aux.item(j).getTextContent();
          } else if (aux.item(j).getNodeName().equals("text")) {
            informationNeeds[indice][1] = aux.item(j).getTextContent();
          }
        }
        indice++;
      }
    }
    FileWriter resultado = new FileWriter(output);
    PrintWriter resultadoWriter = new PrintWriter(resultado);

    for (int i = 0; i < 5; i++) {
      String consulta = informationNeeds[i][1];

      System.out.println("PROCESANDO: " + consulta);
      int desde = -1, hasta = -1;
      Pattern fecha = Pattern.compile("(publicados entre |periodo | a partir de )(?<anyo>\\d\\d\\d\\d)");
      Matcher m = fecha.matcher(consulta);
      while (m.find()) {
        desde = Integer.parseInt(m.group("anyo"));
      }
      fecha = Pattern.compile("(y |-)(?<anyo>\\d\\d\\d\\d)");
      m = fecha.matcher(consulta);
      while (m.find()) {
        hasta = Integer.parseInt(m.group("anyo"));
      }
      if(desde == -1){
        fecha = Pattern.compile("(los últimos )(?<anyo>\\d)");
        m = fecha.matcher(consulta);
        while (m.find()) {
          desde = 2018 - Integer.parseInt(m.group("anyo"));
          hasta = 2018;
        }
      }
      System.out.println("desde: " + desde + " hasta: " + hasta);
      //consulta sobre las fechas encontradas
      Query dateQuery = IntPoint.newRangeQuery("date", Integer.MIN_VALUE, Integer.MAX_VALUE);
      if(desde != -1 && hasta != -1){
        dateQuery = IntPoint.newRangeQuery("date", desde, hasta);
      }
      else if(desde != -1){
        dateQuery = IntPoint.newRangeQuery("date", desde, Integer.MAX_VALUE);
      }
      else if(hasta != -1){
        dateQuery = IntPoint.newRangeQuery("date", Integer.MIN_VALUE, hasta);
      }

      //Sacar nombres propios y consultar sobre creator
      Pattern propio = Pattern.compile("(?<nombre>[Á-ÚA-Z][a-zá-ú]+)");
      m = propio.matcher(consulta);
      String nom="";
      while (m.find()) {
        if(nombres.contains(m.group("nombre"))){
          nom +=  m.group("nombre")+" ";
      }
      System.out.println("nombre: " + nom);
      // Consulta sobre title, subject y description
      QueryParser parserTitle = new QueryParser("title", analyzer);
      Query tituloQuery = parserTitle.parse(consulta);
      QueryParser parserSubject = new QueryParser("subject", analyzer);
      Query subjectQuery = parserSubject.parse(consulta);
      QueryParser parserDescription = new QueryParser("description", analyzer);
      Query descriptionQuery = parserDescription.parse(consulta);

      Builder builderConsulta = new BooleanQuery.Builder()
              .add(new BoostQuery(dateQuery,1f),BooleanClause.Occur.SHOULD)
              .add(new BoostQuery(tituloQuery,1),BooleanClause.Occur.SHOULD)
              .add(new BoostQuery(subjectQuery,1.25f),BooleanClause.Occur.SHOULD)
              .add(new BoostQuery(descriptionQuery,0.4f),BooleanClause.Occur.SHOULD);
     // if(tipoQuery!=null){builderConsulta.add(new BoostQuery(tipoQuery,0.75f),BooleanClause.Occur.SHOULD);}

      if(nom!=""){
        QueryParser parserNombre = new QueryParser("creator", nameAnalyzer);
        Query creator = parserNombre.parse(nom);
        builderConsulta.add(new BoostQuery(creator,5),BooleanClause.Occur.SHOULD);
      }

      Query query = builderConsulta.build();
      TopDocs results = searcher.search(query, Integer.MAX_VALUE);
      ScoreDoc[] hits = results.scoreDocs;
      hits = searcher.search((Query)query, (int)results.totalHits).scoreDocs;
      for(int j=0;j<hits.length;j++){
        Document doc = searcher.doc(hits[j].doc);
        resultadoWriter.println(informationNeeds[i][0] + "\t" + doc.get("path"));
      }
      //System.out.println("----------------------------------------------------");

    }
    reader.close();
    resultadoWriter.close();
  }
}
