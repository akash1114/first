/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.demo1;

//Import necessary library
import java.io.*;
import java.text.ParseException;
import java.util.Scanner;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.json.*;
import weka.core.Instances;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.supervised.attribute.TSLagMaker;

/**
 *
 * @author akash
 */
public class StockPrediction {

    public static void main(String[] args) throws IOException, ParseException, Exception {
        
        Scanner scanner = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter ticker symbol :-");
        String symbol = scanner.nextLine();
        
        Response response = getResponse(symbol);//Gathering Data from API 
        
        setArff(response);//Converting to Arff formate
        
        Instances dataset = new Instances(new BufferedReader(new FileReader("D://stock1.arff")));
        //Instances forecast = new Instances(new BufferedReader(new FileReader("D://stock.arff")));
        dataset.sort(0);
        WekaForecaster forecaster = new WekaForecaster();
        forecaster.setFieldsToForecast("close");//Prediction fild
        forecaster.setBaseForecaster(new GaussianProcesses());
        forecaster.getTSLagMaker().setTimeStampField("date");
        forecaster.getTSLagMaker().setMinLag(12);
        forecaster.getTSLagMaker().setMaxLag(24); 
        forecaster.buildForecaster(dataset);
        forecaster.primeForecaster(dataset);
        DateTime currentDt = getCurrentDateTime(forecaster.getTSLagMaker());

        // forecast units (days) beyond the end of the training data
        List<List<NumericPrediction>> forecast = forecaster.forecast(7, System.out);

        // output the predictions
        for (int i = 0; i < 7; ++i) {
            List<NumericPrediction> predsAtStep = forecast.get(i);
            NumericPrediction predForTarget = predsAtStep.get(0);
            System.out.print(currentDt + " ->> " + predForTarget.predicted() + " ");
            System.out.println();
            currentDt = advanceTime(forecaster.getTSLagMaker(), currentDt);
        }
    }    

    public static void setArff(Response response) throws IOException{
        JSONObject output = new JSONObject(response.peekBody(20000).string());
        JSONArray docs = output.getJSONArray("prices");
        File file = new File("D://Stock1.csv");//Converting to CSV file
        //CSV to Arff formate
        String csv = CDL.toString(docs);
        FileUtils.writeStringToFile(file, csv);
        System.out.println("Data has been Sucessfully Writeen to "+ file);
        
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File("D://Stock1.csv"));
        Instances data = loader.getDataSet();
        
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File("D://stock1.arff"));
        saver.writeBatch();//arff formate 
    }
   //Collection data from API 
    public static Response getResponse(String symbol) throws IOException{
        
        Date today = new Date();
        Calendar cal = new GregorianCalendar();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, -100);
        Date ndays = cal.getTime();
            
        OkHttpClient client = new OkHttpClient();
        //API request
        Request request = new Request.Builder()
	.url("https://apidojo-yahoo-finance-v1.p.rapidapi.com/stock/v2/get-historical-data?frequency=1d&filter=history&period1="+ndays.getTime()/1000+"&period2="+today.getTime()/1000+"&symbol="+symbol)
	.get()
	.addHeader("x-rapidapi-host", "apidojo-yahoo-finance-v1.p.rapidapi.com")
	.addHeader("x-rapidapi-key", "4d1e573c15msh0e14453648cfb18p12542bjsnd198b7a0ae94")
	.build();  
        Response response = client.newCall(request).execute();
        
        return response;
    }
    
    
    private static DateTime getCurrentDateTime(TSLagMaker lm) throws Exception {
    
        return new DateTime((long) lm.getCurrentTimeStampValue());

    }


    private static DateTime advanceTime(TSLagMaker lm, DateTime dt) {
    
        return new DateTime((long) lm.advanceSuppliedTimeValue(dt.getMillis()));

    }    

}
