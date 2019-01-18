package IR.Practica5;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.vocabulary.VCARD;

/**
 * Ejemplo de como construir un modelo de Jena y añadir nuevos recursos 
 * mediante la clase Model
 */
public class A_CreacionRDF {
	
	/**
	 * muestra un modelo de jena de ejemplo por pantalla
	 */
	public static void main (String args[]) {
        Model model = A_CreacionRDF.generarEjemplo();
        // write the model in the standar output
        model.write(System.out); 
    }
	
	/**
	 * Genera un modelo de jena de ejemplo
	 */
	public static Model generarEjemplo(){
		// definiciones
        String personURI    = "http://somewhere/JohnSmith";
        String givenName    = "John";
        String familyName   = "Smith";
        String fullName     = givenName + " " + familyName;

        // crea un modelo vacio
        Model model = ModelFactory.createDefaultModel();

        // le a�ade las propiedades
        Resource johnSmith  = model.createResource(personURI)
             .addProperty(VCARD.FN, fullName)
             .addProperty(VCARD.N, 
                      model.createResource()
                           .addProperty(VCARD.Given, givenName)
                           .addProperty(VCARD.Family, familyName));
        String personURI2  = "http://somewhere/JohnSmith2";
        String givenName2    = "John";
        String familyName2   = "Smith";
        String fullName2     = givenName + " " + familyName;

        // le a�ade las propiedades
        Resource johnSmith2  = model.createResource(personURI2)
                .addProperty(VCARD.FN, fullName2)
                .addProperty(model.createProperty("http://xmlns.com/foaf/0.1/knows"), johnSmith)
                .addProperty(VCARD.N,
                        model.createResource()
                                .addProperty(VCARD.Given, givenName2)
                                .addProperty(VCARD.Family, familyName2));
        johnSmith.addProperty(model.createProperty("http://xmlns.com/foaf/0.1/knows"), johnSmith2);
        model.add
        return model;
	}
	
	
}
