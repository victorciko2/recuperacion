import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;

public class SemanticSearcher {

	/**
	 * obtenerConsultas
	 * 
	 * @param ficheroConsultas
	 * @return
	 */
	private static ArrayList<String> obtenerConsultas(File ficheroConsultas){
		ArrayList<String> consultas = new ArrayList<String>();
		try {
			Scanner scan = new Scanner(ficheroConsultas);
			while(scan.hasNextLine()){
				consultas.add(scan.next());
				consultas.add(scan.nextLine().trim());
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
		return consultas;
		
	}
	
	/**
	 * consultas
	 * 
	 * @param pathModelo
	 * @param ficheroConsultas
	 * @param ficheroSalida
	 */
	private static void consultas(String pathModelo, File ficheroConsultas, File ficheroSalida){
		
		// cargamos el fichero deseado
		Model model = FileManager.get().loadModel(pathModelo);
		
		// obtener las consultas SPARQL
		ArrayList<String> consultas = new ArrayList<String>(); 
		consultas = obtenerConsultas(ficheroConsultas);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida, false));
			
			//Procesar cada consulta
			for(int c=1; c<consultas.size(); c=c+2){
				System.out.println(consultas.get(c));
				Query query = QueryFactory.create(consultas.get(c));
				QueryExecution qexec = QueryExecutionFactory.create(query, model);
				
				try {
				    ResultSet results = qexec.execSelect();
				    for ( ; results.hasNext() ; )
				    {
				      QuerySolution soln = results.nextSolution();
				      System.out.println(soln.toString());
				      Resource x = soln.getResource("x");
				      String resultadoConsulta = x.getURI().substring(24);
				      System.out.println(resultadoConsulta);
				      bw.write(consultas.get(c-1) + "\t" + resultadoConsulta + "\r\n");				      
				    }
				  } finally { qexec.close() ; }
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * main
	 * 
	 * @param args
	 */
	public static void main (String args[]) {
		String pathModelo = null;
		String rdfsPath = null;
		String infoNeeds = null;
		String outputFile = null;
		
		for (int i = 0; i < args.length; i++) {
			if ("-rdf".equals(args[i])) {
				pathModelo = args[i + 1];
				i++;
			}
			else if ("-rdfs".equals(args[i])) {
				rdfsPath = args[i + 1];
				i++;
			}
			else if ("-infoNeeds".equals(args[i])) {
				infoNeeds = args[i + 1];
				i++;
			}
			else if ("-output".equals(args[i])) {
				outputFile = args[i + 1];
				i++;
			}
		}

		File ficheroConsultas = new File(infoNeeds);
		File ficheroSalida = new File(outputFile);
		try {
			consultas(pathModelo, ficheroConsultas, ficheroSalida);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("FIN");
	}
}
