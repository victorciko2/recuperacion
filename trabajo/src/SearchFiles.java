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
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
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
    Analyzer nameAnalyzer = new Analizador();
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    org.w3c.dom.Document document = builder.parse(new File(infoNeeds));
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
      //sacar fechas
      int desde = -1, hasta = -1;
      // Busqueda de fechas en el documento
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
      /*******************************************************************************
       * SI NO SE ESPECIFICA EN LAS CONSULTAS EL TIPO DEL TRABAJO, ESTO SOBRARIA SUPONGO
       ***********************************************************/
      // Sacar tipo de trabajo
     /* String tipo=""; // "" da igual, bachelorthesis o masterthesis
      Pattern ptipo = Pattern.compile("master| tesis de fin de master| tesis de master|trabajo fin de master |trabajo de doctorado|doctora");
      if(ptipo.matcher(consulta).find()){
        tipo="masterthesis";
      }
      else {
        ptipo = Pattern.compile("tfg |trabajo fin de grado");
        if(ptipo.matcher(consulta).find()){
          tipo="bachelorthesis";
        }
      }
      Query tipoQuery=null;
      if(!tipo.equals("")){
        QueryParser parserTipo = new QueryParser("type", analyzer);
        tipoQuery = parserTipo.parse(tipo);
      }
      */

      //Sacar nombres propios y consultar sobre creator
      Pattern propio = Pattern.compile("((?<nombre>\\p{Upper}[a-z]+) )");
      m = propio.matcher(consulta);
      String nom="";
      while (m.find()) {
        if(nombres.contains(m.group("nombre"))){
          nom + =m.group("nombre")+" ";
        }
      }
      System.out.println(nom);
      /*
      // Consulta sobre title, subject y description
      QueryParser parsert = new QueryParser("title", analyzer);
      Query titulo = parsert.parse(consulta);
      QueryParser parsers = new QueryParser("subject", analyzer);
      Query subject = parsers.parse(consulta);
      QueryParser parserd = new QueryParser("description", analyzer);
      Query description = parserd.parse(consulta);
      Builder builderConsulta = new BooleanQuery.Builder()
              .add(new BoostQuery(date,1f),BooleanClause.Occur.SHOULD)
              .add(new BoostQuery(titulo,1),BooleanClause.Occur.SHOULD)
              .add(new BoostQuery(subject,1.25f),BooleanClause.Occur.SHOULD)
              .add(new BoostQuery(description,0.4f),BooleanClause.Occur.SHOULD);
      if(tipoQuery!=null){builderConsulta.add(new BoostQuery(tipoQuery,0.75f),BooleanClause.Occur.SHOULD);}

      if(nom!=""){
        QueryParser parserc = new QueryParser("creator", analyzer);
        Query creator = parserc.parse(nom);
        builderConsulta.add(new BoostQuery(creator,5),BooleanClause.Occur.SHOULD);
      }
      Query query = builderConsulta.build();
      TopDocs results = searcher.search(query, Integer.MAX_VALUE);
      ScoreDoc[] hits = results.scoreDocs;
      hits = searcher.search((Query)query, (int)results.totalHits).scoreDocs;
      for(int j=0;j<hits.length;j++){
        Document doc = searcher.doc(hits[j].doc);
        resulWriter.println(info[i][0] + "\t" + doc.get("name"));
      }
      //System.out.println("----------------------------------------------------");

    }*/
    }
  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   *//*
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = (int)results.totalHits;
    System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    int end = Math.min(numTotalHits, hitsPerPage);
        
    while (true) {
      if (end > hits.length) {
        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
        System.out.println("Collect more (y/n) ?");
        String line = in.readLine();
        if (line.length() == 0 || line.charAt(0) == 'n') {
          break;
        }

        hits = searcher.search(query, numTotalHits).scoreDocs;
      }
      
      end = Math.min(hits.length, start + hitsPerPage);
      
      for (int i = start; i < end; i++) {
        if (raw) {                              // output raw format
          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
          continue;
        }

        Document doc = searcher.doc(hits[i].doc);
        System.out.println(doc.toString());
        String path = doc.get("path");
        if (path != null) {
          System.out.println((i+1) + ". " + path);
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }
                  
      }

      if (!interactive || end == 0) {
        break;
      }

      if (numTotalHits >= end) {
        boolean quit = false;
        while (true) {
          System.out.print("Press ");
          if (start - hitsPerPage >= 0) {
            System.out.print("(p)revious page, ");  
          }
          if (start + hitsPerPage < numTotalHits) {
            System.out.print("(n)ext page, ");
          }
          System.out.println("(q)uit or enter number to jump to a page.");
          
          String line = in.readLine();
          if (line.length() == 0 || line.charAt(0)=='q') {
            quit = true;
            break;
          }
          if (line.charAt(0) == 'p') {
            start = Math.max(0, start - hitsPerPage);
            break;
          } else if (line.charAt(0) == 'n') {
            if (start + hitsPerPage < numTotalHits) {
              start+=hitsPerPage;
            }
            break;
          } else {
            int page = Integer.parseInt(line);
            if ((page - 1) * hitsPerPage < numTotalHits) {
              start = (page - 1) * hitsPerPage;
              break;
            } else {
              System.out.println("No such page");
            }
          }
        }
        if (quit) break;
        end = Math.min(numTotalHits, start + hitsPerPage);
      }
    }
  }*/

}