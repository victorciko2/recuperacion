import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

public class SemanticGenerator {
	
	private static final String STOPWORDS = "stopwords.txt";
	
	
	/**
	* Genera el modelo con el path, el tema, la fecha, el autor y el tipo de trabajo que es.
	* tipo 0 -> TFG, tipo 1-> TFM. tipo 2 -> TESIS
	 */
	private static Model generarModelo(String path,  ArrayList<String> temas, int fecha, 
			 ArrayList<String> autores, int tipo){
		//MODELO VACIO
        Model model = ModelFactory.createDefaultModel();
        
        // Creamos las propiedades del modelo
        Property propTema = model.createProperty("http://www.equipo03.com/Tema");
        Property propAutor = model.createProperty("http://www.equipo03.com/Autor");
        Property propFecha = model.createProperty("http://www.equipo03.com/Fecha");
        Property nombreCreator = model.createProperty("http://www.equipo03.com/Autor/Nombre");
        Property apellido1Creator = model.createProperty("http://www.equipo03.com/Autor/Primer_Apellido");
        Property apellido2Creator = model.createProperty("http://www.equipo03.com/Autor/Segundo_Apellido");
        
        Scanner scan = new Scanner(path);
        scan.useDelimiter("\\\\");
        scan.next();
        path=scan.nextLine();
        scan.close();
        path=path.substring(1);
        // Creamos el recurso  añadimos us propiedades
        Resource TrabajoAcademico = model.createResource("http://www.equipo03.com/"+path)
        		.addLiteral(propFecha, fecha);
        
        for(int i=0;i<temas.size();i++){
        	TrabajoAcademico.addProperty(propTema, model.createResource(temas.get(i)));
        }
        // Anyadir propiedades creator
        for(int i=0; i<autores.size();i++){
        	String autore = autores.get(i) + ",";
        	System.out.println(autore);
			Scanner scannerCreator = new Scanner(autore);
			scannerCreator.useDelimiter(",");
			String apellidosCreador = scannerCreator.next();
			String nombreCreador = scannerCreator.next();
			scannerCreator.close();
	
			nombreCreador = nombreCreador.trim();
			nombreCreador = nombreCreador.replace(" ", "_");
	        nombreCreador = cleanString(nombreCreador);

	        apellidosCreador = apellidosCreador.trim();
	        apellidosCreador = cleanString(apellidosCreador);
	        Scanner ap2 = new Scanner(apellidosCreador);
	        String apellido11 = "";
	        if(ap2.hasNext()){
	        	apellido11 = ap2.next();
	        }
	        String apellido22 = "";
	        if(ap2.hasNext()){
	        	apellido22 = ap2.next();
	        }
	        ap2.close();
	        
	        TrabajoAcademico = TrabajoAcademico.addProperty(propAutor, 
    				model.createResource("http://www.equipo03.com/"+nombreCreador + "_" + apellido11 + "_" + apellido22)
						.addProperty(nombreCreator, nombreCreador)
						.addProperty(apellido1Creator, apellido11)
						.addProperty(apellido2Creator, apellido22));
        }        
        return model;
	}
		

	private static String procesarTexto(String line) throws ParseException {
		String aux=line;
		return aux;
	}
	
	
	/**
	 * generarRDF
	 * 
	 * @param file
	 * @param conceptos
	 * @param ficheroSalida
	 * @param ModeloFINAL
	 * @throws IOException
	 */
	private static void generarRDF(File file, ArrayList<String> conceptos, File ficheroSalida, Model ModeloFINAL) throws IOException {
		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				File[] files = file.listFiles();
				// an IO error could occur
				if (files != null) {
					System.out.println(files.length);
					for (int i = 0; i < files.length; i++) {
						generarRDF(files[i], conceptos, ficheroSalida, ModeloFINAL);
					}
					// ESCRIBIR FICHERO TTL
					try {
						ModeloFINAL.write(new FileOutputStream(ficheroSalida, false), "TTL");						
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("FIN");
				}
			} else {
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException fnfe) {
					// at least on windows, some temporary files raise this
					// exception with an "access denied" message
					// checking if the file can be read doesn't help
					return;
				}

				try {
					
					String path = file.getPath();
					System.out.println(path);
					String titulo = null;
					int fecha = 0;
					String descripcion = null;
					String materias = null;
					int tipo=0;
					String tipoTrab= null;
					
					ArrayList<String> creatorS = new ArrayList<String>();
					
					// RDF FECHA
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					org.w3c.dom.Document doc2 = dBuilder.parse(file);

					doc2.getDocumentElement().normalize();

					NodeList nList = doc2.getElementsByTagName("dc:date");
					Node nNode = nList.item(0);
					if(nNode != null){
						String texto = nNode.getTextContent();
						fecha = Integer.parseInt(texto);
					}


					// RDF TITULO
					nList = doc2.getElementsByTagName("dc:title");
					for (int i = 0; i < nList.getLength(); i++) {
						nNode = nList.item(i);
						titulo = nNode.getTextContent();
					}
					
					// Quitar StopWords, acentos y pasar a minusculas el titulo
					titulo = procesarTexto(titulo);
					titulo = titulo.toLowerCase();
					titulo = cleanString(titulo);
					// RDF DESCRIPCION
					nList = doc2.getElementsByTagName("dc:description");
					for (int i = 0; i < nList.getLength(); i++) {
						nNode = nList.item(i);
						descripcion = nNode.getTextContent();
					}
					
					// Quitar StopWords, acentos y pasar a minusclas la descripcion
					descripcion = procesarTexto(descripcion);
					descripcion = descripcion.toLowerCase();
					descripcion = cleanString(descripcion);
					
					nList = doc2.getElementsByTagName("dc:subject");
					for (int i = 0; i < nList.getLength(); i++) {
						nNode = nList.item(i);
						materias = nNode.getTextContent();
					}
					if(materias!=null){
						materias = procesarTexto(materias);
						materias = materias.toLowerCase();
						materias = cleanString(materias);
					}
					else{
						materias="";
					}
					// RDF TIPO
					nList = doc2.getElementsByTagName("dc:type");
					for (int i = 0; i < nList.getLength(); i++) {
						nNode = nList.item(i);
						tipoTrab = nNode.getTextContent();
					}
					if(tipoTrab.equals("info:eu-repo/semantics/masterThesis")){
						tipo = 1;
					}
					else if(tipoTrab.equals("info:eu-repo/semantics/bachelorThesis")){
						tipo = 0;
					}
					else{
						tipo = 2;
					}
					// RDF CREADOR
					nList = doc2.getElementsByTagName("dc:creator");
					for (int i = 0; i < nList.getLength(); i++) {
						nNode = nList.item(i);
						creatorS.add(nNode.getTextContent());
					}
													
					// TEMA DEL DOCUMENTO
					ArrayList<String> al= new ArrayList<String>();
					for(int i=1; i<conceptos.size();i=i+2){
						if(titulo.contains(procesarTexto(conceptos.get(i).toLowerCase())) || 
								materias.contains(procesarTexto(conceptos.get(i).toLowerCase())) ||
								(descripcion.contains(procesarTexto(conceptos.get(i).toLowerCase())))){
							al.add(conceptos.get(i-1));
						}
					}
					if(al.size()==0){
						al.add("http://www.equipo03.com/Otros");
					}
					//System.out.println(materias);
					// Crear Modelo
					Model modeloPro = generarModelo(path, al, fecha, creatorS,tipo);
					System.out.println(modeloPro.isEmpty());
					if(modeloPro.isEmpty()){
						System.out.println("POLLAS");
						throw new Exception();
					}
					// Añadir modelo al modelo final
					ModeloFINAL.add(modeloPro);
				}	 
				catch (Exception e) {
					e.printStackTrace();
				}
				finally {
					fis.close();
				}
			}
		}
			
	}
	
	/**
	 * cleanString
	 * 
	 * @param texto
	 * @return
	 */
	private static String cleanString(String texto) {
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return texto;
    }
	
	/**
	 * obtenerConceptos
	 * 
	 * @param ficheroSKOS
	 * @return
	 */
	private static ArrayList<String> obtenerConceptos(String ficheroSKOS){
		ArrayList<String> conceptos = new ArrayList<String>();
		
		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel(ficheroSKOS);

		// obtenemos todos los statements del modelo
		StmtIterator it = model.listStatements();

		// mostramos todas las tripletas cuyo objeto es un literal
		while (it.hasNext()) {
			Statement st = it.next();
			if (st.getObject().isLiteral()) {
				conceptos.add(st.getSubject().getURI());
				conceptos.add(st.getLiteral().toString());
			}
		}
		
		return conceptos;
	}
	
	/**
	 * main
	 * 
	 * @param args
	 */
	public static void main (String args[]) {
		String rdfPath = null;
		String skosPath = null;
		String docsPath = null;
		
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
		Model modeloFINAL = ModelFactory.createDefaultModel();
		// Añade los recursos skos
		
		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel(skosPath);
		// obtenemos todos los statements del modelo
		StmtIterator it = model.listStatements();
		// mostramos todas las tripletas cuyo objeto es un literal
		String ant = "";
		Resource actual=null;
		Property prefLabel = modeloFINAL.createProperty("PrefLabel");
		Property altLabel = modeloFINAL.createProperty("AltLabel");
		Property narrower = modeloFINAL.createProperty("Narrower");
		Property broader = modeloFINAL.createProperty("Broader");
		while (it.hasNext()) {
			Statement st = it.next();
			if(!ant.equals(st.getSubject().getURI())){
				actual = modeloFINAL.createResource(st.getSubject().getURI());
			}
			ant = st.getSubject().getURI();
			if(st.getPredicate().toString().equals("http://www.w3.org/TR/2009/NOTE-skos-primer-20090818/broader")){
				//etiqueta broader
				actual.addProperty(broader,st.getObject());
			}
			else if(st.getPredicate().toString().equals("http://www.w3.org/TR/2009/NOTE-skos-primer-20090818/narrower")){
				//etiqueta narrower
				actual.addProperty(narrower,st.getObject());
			}
			else if(st.getPredicate().toString().equals("http://www.w3.org/TR/2009/NOTE-skos-primer-20090818/prefLabel")){
				//etiqueta prefLabel
				actual.addProperty(prefLabel,st.getObject());
			}else if(st.getPredicate().toString().equals("http://www.w3.org/TR/2009/NOTE-skos-primer-20090818/altLabel")){
				//etiqueta altLabel
				actual.addProperty(altLabel,st.getObject());
			}
			System.out.println("_________________________");
			System.out.println(st.toString());			
			System.out.println();
		}
		
		File ficheroSalida = new File(rdfPath);
		try {
			
			// Obtener los conceptos del modelo 
			ArrayList<String> conceptos = new ArrayList<String>();
			conceptos = obtenerConceptos(skosPath);
			// generar RDF
			generarRDF(docDir, conceptos, ficheroSalida, modeloFINAL);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
