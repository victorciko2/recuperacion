package Semantico;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;

public class SemanticSearcher {

    static String prefix = "PREFIX ns: <http://www.07.com/> PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    public static void ejecutarConsultas (ArrayList<String> consultas, File ficheroSalida, Model model) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(ficheroSalida, false));
        String zaguan = "oai_zaguan.unizar.es_";

        for (int i = 0; i < consultas.size(); i = i+2){
            String idQuery = consultas.get(i);
            String query = consultas.get(i + 1);
            Query q  = QueryFactory.create(prefix + query);

            QueryExecution qe = QueryExecutionFactory.create(q, model);
            ResultSet results = qe.execSelect();
            while (results.hasNext()){
                QuerySolution sol = results.nextSolution();
                String r = sol.getLiteral("id").toString();
                String idFinal = r.substring(r.lastIndexOf("/") + 1);
                idFinal = zaguan + idFinal + ".xml";
                bw.write(idQuery + " " + idFinal + "\n");
            }
            qe.close();
        }
        bw.close();
    }

    public static ArrayList<String> obtenerQueries (File f) throws IOException {
        ArrayList<String> resultado = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null){
            String id = line.substring(0, line.indexOf(" "));
            String query = line.substring(line.indexOf(" ") + 1);
            resultado.add(id);
            resultado.add(query);
        }
        return resultado;
    }

    public static void main (String args[]) throws IOException, SAXException, ParserConfigurationException {
        String rdfPath = "salidaBuena.ttl";
        String rdfsPath = "";
        String infoNeeds = "queries.txt";
        String output = "salidaFinalFinalFinalisimaDeLaMuerta1234.txt";

        for (int i = 0; i < args.length; i++) {
            if ("-rdf".equals(args[i])) {
                rdfPath = args[i + 1];
                i++;
            } else if ("-rdfs".equals(args[i])) {
                rdfsPath = args[i + 1];
                i++;
            } else if ("-infoNeeds".equals(args[i])) {
                infoNeeds = args[i + 1];
                i++;
            } else if ("-output".equals(args[i])) {
                output = args[i + 1];
                i++;
            }
        }

        File fileQueries = new File(infoNeeds);
        ArrayList<String> resultado = obtenerQueries(fileQueries);

        Model model = FileManager.get().loadModel(rdfPath);
        File ficheroSalida = new File(output);
        ejecutarConsultas(resultado, ficheroSalida, model);
    }


}
