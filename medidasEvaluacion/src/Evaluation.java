import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.io.FileWriter;
import java.io.PrintWriter;

public class Evaluation {
	static int k=50;
	public static void main(String[] args) throws IOException{
		String qrelsFile = "", resultsFile = "", output="";
		if(args.length != 6){ //comprobar parametros
			System.out.println("Uso: java Evaluation -qrels qrels.txt -results results.txt -output outupFile.txt");
			System.exit(-1);
		}
		qrelsFile = args[1];
		resultsFile = args[3];
		output = args[5];
		Map<String,Map<String, Boolean>> qrels = new HashMap<String,Map<String, Boolean>>();

		//abrir fichero escritura
		FileWriter file = new FileWriter(output);
		PrintWriter writer = new PrintWriter(file);

		//abrir zaguanRels
		FileInputStream fstream = new FileInputStream(qrelsFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
		String strLine = "";
		//leer linea por linea
		while ((strLine = br.readLine()) != null)   {
			String[] parts = strLine.split("\t");
			if(!qrels.containsKey(parts[0])){
				qrels.put(parts[0], new HashMap<String,Boolean>());
			}
			qrels.get(parts[0]).put(parts[1], parts[2].equals("1"));
		}
		br.close();
		
		Map<String,ArrayList<String>> results = new HashMap<String,ArrayList<String>>();
		
		fstream = new FileInputStream(resultsFile);
		br = new BufferedReader(new InputStreamReader(fstream));
		while ((strLine = br.readLine()) != null)   {
			String[] parts=strLine.split("\t");
			if(!results.containsKey(parts[0])){
				results.put(parts[0], new ArrayList<String>());
			}
			results.get(parts[0]).add(parts[1]);
		}
		br.close();
		double[] precTotal=new double[results.size()];
		double[] recTotal=new double[results.size()];
		double[] prec10Total=new double[results.size()];
		double[][] precITotal=new double[results.size()][11];
		int indiceTotal=0;
		double[] meanAverage=new double[results.size()];
		for (Map.Entry<String, ArrayList<String>> entry : results.entrySet()){
			int tp = 0, fp = 0, tn = 0, fn = 0, MAX = 0;
			System.out.println("INFORMATION_NEED: " + entry.getKey());
			writer.println("INFORMATION_NEED: " + entry.getKey());
			ArrayList<String> docsi=entry.getValue();
		    if(docsi.size() < 45) MAX = docsi.size();
		    else MAX = 44;

			List<String> docs = docsi.subList(0, MAX);
		    Map<String,Boolean> qrelsNeed = qrels.get(entry.getKey());
		    for(int i = 0; i < docs.size(); i++){
		    	if(!qrelsNeed.containsKey(docs.get(i))){
		    		fp++;
		    	}
		    }
		    for (Map.Entry<String, Boolean> entradas : qrelsNeed.entrySet()){
		    	if(entradas.getValue() && !docs.contains(entradas.getKey())){
		    		fn++;
		    	}
		    	else if(entradas.getValue() && docs.contains(entradas.getKey())){
		    		tp++;
		    	}
		    	else if(!entradas.getValue() && docs.contains(entradas.getKey())){
		    		fp++;
		    	}
		    	else if(!entradas.getValue() && !docs.contains(entradas.getKey())){
		    		tn++;
		    	}
		    }
		    double precision = tp / (1.0* (tp + fp));
		    double recall= tp / (1.0 * (tp + fn));
		    DecimalFormat df = new DecimalFormat("0.000");
			System.out.println("precision: " + df.format(precision));
			System.out.println("recall: " + df.format(recall));
			System.out.println("F1: " + df.format((2*precision*recall)/(precision+recall)));
			precTotal[indiceTotal] = precision;
			recTotal[indiceTotal] = recall;
			writer.println("precision: " + df.format(precision));
			writer.println("recall: " + df.format(recall));
		    writer.println("F1: " + df.format((2 * precision * recall) / (precision + recall)));
		    //PREC @ 10
		    int tp10 = 0, fp10 = 0;
		    for(int i = 0; i < docs.size() && i < 10; i++){
		    	if(!qrelsNeed.containsKey(docs.get(i)))	fp10++;
		    	else if(qrelsNeed.get(docs.get(i)))	tp10++;
		    	else if(!qrelsNeed.get(docs.get(i)))	fp10++;
		    }
		    System.out.println("prec@10 " + df.format(tp10 * 1.0 / (tp10 + fp10)));
		    writer.println("prec@10 " + df.format(tp10*1.0/(tp10+fp10)));
		    //average_precision
		    prec10Total[indiceTotal] = tp10*1.0/(tp10+fp10);
		    int tpA = 0, fpA = 0;
		    double precMedia = 0;
		    for(int i = 0; i < docs.size(); i++){
		    	if(!qrelsNeed.containsKey(docs.get(i)))	fpA++;
		    	else if(qrelsNeed.get(docs.get(i))){
		    		tpA++;
		    		precMedia += tpA * 1.0 / (tpA + fpA);
		    	}
		    	else if(!qrelsNeed.get(docs.get(i)))	fpA++;
		    }
		    if(tpA != 0)	precMedia = precMedia / tpA;
		    else	precMedia = 0;
		    System.out.println("average_precision " + df.format(precMedia));
		    writer.println("average_precision " + df.format(precMedia));
		    meanAverage[indiceTotal] = precMedia;
		    int tprp = 0, fprp = 0;
		    int dr = 0;
		    System.out.println("recall_precision");
		    writer.println("recall_precision");
		    for (Map.Entry<String, Boolean> entradas : qrelsNeed.entrySet()){
		    	if(entradas.getValue())	dr++;
		    }
		    double[] rec = new double[10], prec = new double[10];
		    int in = 0;
		    for(int i = 0; i < docs.size() && in < 10; i++){
		    	if(!qrelsNeed.containsKey(docs.get(i)))	fprp++;
		    	else if(qrelsNeed.get(docs.get(i))){
		    		tprp++;
			    	rec[in] = tprp/(1.0*(dr));
			    	prec[in] = tprp/(1.0*(tprp+fprp));
			    	System.out.println(df.format(rec[in]) + " " + df.format(prec[in]));
			    	writer.println(df.format(rec[in]) + " " + df.format(prec[in]));
			    	in++;
		    	}
		    	else if(!qrelsNeed.get(docs.get(i)))	fprp++;
		    }
		    System.out.println("interpolated_recall_precision");
		    writer.println("interpolated_recall_precision");

		    double[] recI = new double[11],precI=new double[11];
		    for(int i = 0; i <= 10; i++){
				precI[i]=0;
				recI[i]=i*0.1;
		    }
		    for(int j = 0; j < 10; j++){
		    	boolean fin = false;
		    	int tpI = 0,fpI = 0;
			    for(int i = 0; i < docs.size() && !fin; i++){
			    	if(!qrelsNeed.containsKey(docs.get(i)))	fpI++;
			    	else if(qrelsNeed.get(docs.get(i))){
						tpI++;
				    	double ex = tpI * 1.0 / dr;
				    	double pre = tpI * 1.0 / (tpI + fpI);
				    	if(pre > precI[j] && recI[j] <= ex)	precI[j] = pre;
			    	}
			    	else if(!qrelsNeed.get(docs.get(i)))	fpI++;
			    }
		    }
		    for(int i = 0; i <= 10; i++){
		    	System.out.println(df.format(recI[i]) + " " + df.format(precI[i]));
		    	writer.println(df.format(recI[i]) + " " + df.format(precI[i]));
		    	precITotal[indiceTotal][i] = precI[i];
		    }
		    indiceTotal++;
		    System.out.println();
		    writer.println();
		}
		System.out.println("TOTAL");
		writer.println("TOTAL");
		double precFinal = 0, recFinal = 0, prec10Final = 0, map = 0;
		for(int i = 0; i < indiceTotal; i++){
			precFinal += precTotal[i];
			recFinal += recTotal[i];
			prec10Final += prec10Total[i];
			map += meanAverage[i];
		}
		precFinal /= indiceTotal;
		recFinal /= indiceTotal;
		prec10Final /= indiceTotal;
		map /= indiceTotal;
	    DecimalFormat df = new DecimalFormat("0.000"); 
		System.out.println("precision: " + df.format(precFinal));
		writer.println("precision: " + df.format(precFinal));
		System.out.println("recall: " + df.format(recFinal));
		writer.println("recall: " + df.format(recFinal));
		System.out.println("F1: " + df.format((2*precFinal*recFinal)/(precFinal+recFinal)));
		writer.println("F1: " + df.format((2*precFinal*recFinal)/(precFinal+recFinal)));
		System.out.println("prec@10: " + df.format(prec10Final));
		writer.println("prec@10: " + df.format(prec10Final));
		System.out.println("MAP: " + df.format(map));
		writer.println("MAP: " + df.format(map));
		
		for(int i = 0; i <= 10; i++){
			for(int j = 1; j < indiceTotal; j++){
				precITotal[0][i] += precITotal[j][i];
			}
			precITotal[0][i] /= indiceTotal;
	    }
		
		System.out.println("interpolated_recall_precision");
		writer.println("interpolated_recall_precision");
		for(int i = 0; i <= 10;i++){
			System.out.println(df.format(i*0.1) + " " + df.format(precITotal[0][i]));
			writer.println(df.format(i*0.1) + " " + df.format(precITotal[0][i]));
		}
	writer.close();
	}
}
