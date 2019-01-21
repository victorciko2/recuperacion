package Semantico;

import org.apache.jena.base.Sys;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

public class SemanticGenerator {

    private static int cont = 0;
    private static ArrayList<String[]> matriz = new ArrayList<String[]>();
    private static File stopWords = new File("D:\\Victor\\7CUATRI\\RI\\recuperacion\\practica5\\src\\main\\java\\Semantico\\stopwords-es.txt");
    private static Set<String> stopWordsSet = new HashSet<String>();

    private static String limpiarTexto(String s){
        return s.toLowerCase().replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .replace("ü", "u")
                .replace("!", " ")
                .replace("¡", " ")
                .replace("?", " ")
                .replace("¿", " ")
                .replace(")", " ")
                .replace("(", " ")
                .replace(".", " ")
                .replace(",", " ")
                .replace(";", " ")
                .replace("[", " ")
                .replace("]", " ")
                .replace("-", " ")
                .replace(":", " ")
                .replace("/", " ")
                .replace("$", " ")
                .replace("%", " ")
                .replace("€", " ")
                .replace("\"", " ")
                .replace("'", " ")
                .replace("=", " ")
                .replace("+", " ")
                .replace("-", " ")
                .replace("“", " ")
                .replace("”", " ")
                .replace("<", " ")
                .replace(">", " ")
                .replace("\t", " ")
                .replace("*", " ")
                .replaceAll("…", " ")
                .replaceAll("[0-9]+", " ")
                .replaceAll("[ ]+", " ");
    }

    private static Model generarModelo(String path, int fecha, ArrayList<String> subjects,
                                       String titulo, String publisher, String descripcion,
                                       String formato, String leng, int tipo, String derechos, // Master tesis = 0, Bachelor tesis = 1, Else = 2
                                       ArrayList<String> autores, String id, ArrayList<Concepto> c){
        Model model = ModelFactory.createDefaultModel();

        // Creamos las propiedades del modelo
        Property propName = model.createProperty("http://www.07.com/Name");
        Property propCreator = model.createProperty("http://www.07.com/Creator");
        Property propTitle = model.createProperty("http://www.07.com/Title");
        Property propIdentifier = model.createProperty("http://www.07.com/Identifier");
        Property propSubject = model.createProperty("http://www.07.com/Subject");
        Property propPublisher = model.createProperty("http://www.07.com/Publisher");
        Property propDate = model.createProperty("http://www.07.com/Date");
        Property propDescription = model.createProperty("http://www.07.com/Description");
        Property propFormat = model.createProperty("http://www.07.com/Format");
        Property propLanguage = model.createProperty("http://www.07.com/Language");
        Property propType = model.createProperty("http://www.07.com/Type");
        Property propRights = model.createProperty("http://www.07.com/Rights");

        String nameFile = path.substring(path.lastIndexOf("\\") + 1);

        Resource type = null;
        if (tipo == 0){
            type = model.createResource("http://www.07.com/Tfm");
        }
        else if (tipo == 1){
            type = model.createResource("http://www.07.com/Tfg");
        }
        else{
            type = model.createResource("http://www.07.com/Thesis");
        }

        Resource documento = model.createResource("http://www.07.com/" + nameFile)
                                    .addLiteral(propDate, fecha)
                                    .addLiteral(propIdentifier, id)
                                    .addLiteral(propTitle, titulo)
                                    .addLiteral(propPublisher, publisher)
                                    .addLiteral(propDescription, descripcion)
                                    .addLiteral(propFormat, formato)
                                    .addLiteral(propLanguage, leng)
                                    .addProperty(propType, type)
                                    .addLiteral(propRights, derechos);

        for (int i = 0; i < autores.size(); i++){
            String nombreCompleto = autores.get(i);
            nombreCompleto = nombreCompleto.replaceAll(",", "");
            nombreCompleto = nombreCompleto.replaceAll(" ", "_");

            documento = documento.addProperty(propCreator,
                    model.createResource("http://www.07.com/") + nombreCompleto)
                            .addLiteral(propName, nombreCompleto);
        }

        String todo = titulo + " " + descripcion;
        for (String subject : subjects){
            todo += " " + subject;
        }
        for (Concepto concepto : c){
            String s = buscar(concepto, todo);
            if (!s.equals("")){
                documento = documento.addProperty(propSubject, model.createResource("http://www.07.com/" + s));
            }
        }
        return model;
    }

    public static void generarRDF(File file, File ficheroSalida, ArrayList<Concepto> c) throws ParserConfigurationException, IOException, SAXException {
        String path = file.getPath();
        String titulo = null, id = null, publisher = null, descripcion = null,
                formato = null, lenguaje = null, tipo = null, derechos = "";
        int fecha = 0, tipoTrabajo = 2;
        ArrayList<String> autores = new ArrayList<String>();

        // Para el tratamiento de XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(file);

        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName("dc:date");
        Node node = nodes.item(0); // Esto devuelve la fecha del documento actual
        if (node != null){
            fecha = Integer.parseInt(node.getTextContent());
        }

        nodes = doc.getElementsByTagName("dc:title");
        node = nodes.item(0);
        if (node != null){
            titulo = node.getTextContent();
            titulo = limpiarTexto(titulo);
        }

        nodes = doc.getElementsByTagName("dc:identifier");
        node = nodes.item(0);
        if (node != null){
            id = node.getTextContent();
        }

        nodes = doc.getElementsByTagName("dc:publisher");
        node = nodes.item(0);
        if (node != null){
            publisher = node.getTextContent();
            publisher = limpiarTexto(publisher);
        }

        nodes = doc.getElementsByTagName("dc:description");
        node = nodes.item(0);
        if (node != null){
            descripcion = node.getTextContent();
            descripcion = limpiarTexto(descripcion);
        }

        nodes = doc.getElementsByTagName("dc:format");
        node = nodes.item(0);
        if (node != null){
            formato = node.getTextContent();
        }

        nodes = doc.getElementsByTagName("dc:language");
        node = nodes.item(0);
        if (node != null){
            lenguaje = node.getTextContent();
        }

        nodes = doc.getElementsByTagName("dc:type");
        node = nodes.item(0);
        if (node != null){
            tipo = node.getTextContent();
        }
        tipo = tipo.substring(tipo.lastIndexOf("/") + 1);
        // Comprobar si es tipo master o bachelor
        if (tipo != null && tipo.equals("masterThesis")){
            tipoTrabajo = 0;
        }
        else if (tipo != null && tipo.equals("bachelorThesis")){
            tipoTrabajo = 1;
        }

        nodes = doc.getElementsByTagName("dc:creator");
        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            autores.add(limpiarTexto(node.getTextContent()));
        }

        nodes = doc.getElementsByTagName("dc:rights");
        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            if ( i == (nodes.getLength() - 1)){
                derechos += node.getTextContent();
            }
            else {
                derechos += node.getTextContent() + ", ";
            }
        }

        Model model = generarModelo(path, fecha, new ArrayList<String>(), titulo, publisher, descripcion,
                formato, lenguaje, tipoTrabajo, derechos, autores, id, c);

        model.write(new FileOutputStream(ficheroSalida, true), "TTL");
    }

    private static void recorrerDirectorio(final File path, File ficheroSalida, ArrayList<Concepto> c) throws IOException, SAXException, ParserConfigurationException {
        for (final File ficheroEntrada : path.listFiles()) {
            cont++;
            if (ficheroEntrada.isDirectory()) { // Entrar en el directorio
                recorrerDirectorio(ficheroEntrada, ficheroSalida, c);
            } else {
                // Trabajar con el fichero file
                generarRDF(ficheroEntrada, ficheroSalida, c);
            }
        }
    }

    private static Model crearTesauro(Model m) throws FileNotFoundException {
        // Obtener la lista del modelo
        StmtIterator iterator = m.listStatements();

        // Se crea el modelo final
        Model modeloFinal = ModelFactory.createDefaultModel();
        Property prefLabel = modeloFinal.createProperty("prefLabel"); // Etiqueta preferida
        Property altLabel = modeloFinal.createProperty("altLabel"); // Etiqueta alternativa
        Property narrower = modeloFinal.createProperty("narrower"); // Significado mas especifico
        Property broader = modeloFinal.createProperty("broader"); // Significado mas general

        String s = "";
        Resource resource = null;
        // Se recorre la lista del modelo que se le pasa
        while (iterator.hasNext()) {
            Statement stat = iterator.next(); // Se obtiene el siguiente
            if (!s.equals(stat.getSubject().getURI())) {
                s = stat.getSubject().getURI();
                resource = modeloFinal.createResource(stat.getSubject().getURI());
            }

            String predicate = stat.getPredicate().toString();
            // Esto nos da http://www.w3.org/TR/2009/NOTE-skos-primer-20090818/... y nos quedamos con lo ultimo
            String tipo = predicate.substring(predicate.lastIndexOf("/") + 1);
            switch (tipo){
                case "prefLabel":
                    resource.addProperty(prefLabel, stat.getObject());
                    break;
                case "altLabel":
                    resource.addProperty(altLabel, stat.getObject());
                    break;
                case "narrower":
                    resource.addProperty(narrower, stat.getObject());
                    break;
                case "broader":
                    resource.addProperty(broader, stat.getObject());
                    break;
            }
        }
        return modeloFinal;
    }

    private static String buscar(Concepto c, String texto){
        String resultado ="";
        for (String a : c.prefLabel){
            if(texto.contains(a)) {
                return c.isRelated(a);
            }
        }
        for (String a : c.altLabel){
            if(texto.contains(a)){
                return c.isRelated(a);
            }
        }
        // Devuele el STRING de la URI del concepto
        return resultado;
    }

    public static void main (String args[]) throws IOException, SAXException, ParserConfigurationException {
        String rdfPath = "salida.txt";
        File rdfSalida = new File(rdfPath);
        String skosPath = "tesauro.n3";
        String docsPath = "D:\\Victor\\7CUATRI\\RI\\recuperacion\\trabajo\\prueba\\";

        for (int i = 0; i < args.length; i++) {
            if ("-rdf".equals(args[i])) {
                rdfPath = args[i + 1];
                i++;
            }
            else if ("-skos".equals(args[i])) {
                skosPath = args[i + 1];
                i++;
            }
            else if ("-docs".equals(args[i])) {
                docsPath = args[i + 1];
                i++;
            }
        }

        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out.println("Document directory '" + docDir.getAbsolutePath()
                    + "' does not exist or is not readable, please check the path");
            System.exit(1);
        }

        /**
         *
         * SKOS MODEL
         */
        Model modeloSkos = FileManager.get().loadModel(skosPath,"TTL");
        // Iterador sobre para identificar todos los conceptos
        ArrayList<Concepto> conceptos = new ArrayList<Concepto>();
        ResIterator it = modeloSkos.listResourcesWithProperty(RDF.type, SKOS.Concept);
        while (it.hasNext()) {
            Resource concept = it.next();
            String path[] = concept.getURI().split("/");
            Concepto nuevo = new Concepto(path[path.length - 1]);
            conceptos.add(nuevo);
        }
        // Añadimos las propiedades de skos al modelo final
        StmtIterator it1 = modeloSkos.listStatements();
        String aux = "";
        Concepto nuevo = null;
        while (it1.hasNext()) {
            Statement st = it1.next();
            if (!aux.equals(st.getSubject().getURI())) {
                String[] aux2 = st.getSubject().getURI().split("/");
                String name=aux2[aux2.length-1];
                for(Concepto i : conceptos)
                    if(i.equals(name)) {
                        nuevo = i;
                        break;
                    }
            }
            switch (st.getPredicate().toString()) {
                case "http://www.w3.org/2004/02/skos/core#narrower":
                    String[] aux2 = st.getObject().asResource().getURI().split("/");
                    for(Concepto i : conceptos)
                        if(i.equals(aux2[aux2.length-1])) {
                            nuevo.addNarrower(i);
                            break;
                        }
                    break;
                case "http://www.w3.org/2004/02/skos/core#altLabel":
                    nuevo.addAltLabel(st.getObject().toString());
                    break;
                case "http://www.w3.org/2004/02/skos/core#prefLabel":
                    nuevo.addPrefLabel(st.getObject().toString());
                    break;
                case "http://www.w3.org/2004/02/skos/core#broader":
                    String[] aux3 = st.getObject().asResource().getURI().split("/");
                    for(Concepto i : conceptos)
                        if(i.equals(aux3[aux3.length-1])) {
                            nuevo.addBroader(i);
                            break;
                        }
                    break;
            }
            aux = st.getSubject().getURI();
        }

        System.out.println("Tamaño de conceptos: " + conceptos.size());
        recorrerDirectorio(docDir, rdfSalida, conceptos);

        System.out.println("Total: " + cont);
    }
}
