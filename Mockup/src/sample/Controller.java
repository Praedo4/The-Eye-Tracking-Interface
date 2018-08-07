package sample;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import sample.ET.ETCollection;
import sample.ET.ETReader;
import sample.ET.GazePoint;
import sample.FS.FSCollection;
import sample.FS.FSEvent;
import sample.FS.FSReader;


import java.util.*;
import java.io.*;

public class Controller {

    @FXML private TextField label;
    @FXML private Canvas surface;
    @FXML private RadioButton eventsRadioButton;
    @FXML private RadioButton savedEventsRadioButton;
    @FXML private RadioButton gazePointsRadioButton;
    @FXML private RadioButton dispersionRadioButton;
    @FXML private RadioButton durationRadioButton;
    @FXML private ListView<String> userList;
    @FXML private ListView<Integer> textList;
    @FXML private TextField dispersionTextBox;
    @FXML private TextField durationTextBox;
    @FXML private TextField minGapSizeTextBox;
    @FXML private TextField maxGapSizeTextBox;
    @FXML private TextField sgolayOrderTextBox;
    @FXML private TextField sgolayLengthTextBox;
    @FXML private TextField timeMissingTextBox;
    @FXML private TextField timeMissingSamplesTextBox;
    @FXML private TextField lineSaccadesTextBox;
    @FXML private TextField outliersTextBox,outliersProportionTextBox;
    @FXML private CheckBox interpolationCheckBox;
    @FXML private CheckBox sgolayCheckBox;
    @FXML private CheckBox cleanOutliersCheckBox;
    @FXML private Slider speedSlider;
    @FXML private Button calculateMeasuresButton;
    private GraphicsContext gc;
    private Random randomNum;
    private Timer tm;
    int index = 0;
    String fileName = "data/raw_data/p01_ET_samples.txt";
    private ETCollection rawData;
    private FSCollection eventData;
    int mode;
    boolean durationDisplay = false;
    final int RAW = 0, EVENTS = 1, TET = 2;
    int selectedTextID = -1, selectedUserID = -1;
    long durationLeft = 0;
    int idtDispersion, idtDuration;
    boolean isIDT = false, cleanOutliers = false;
    Map<Integer, String> imageFilenames;
    int minGapSize = 3, maxGapSize = 10; boolean interpolation = false, sgolay = false; int sgolayOrder = 2, sgolayLength = 5, bb_x = 320, bb_y = 90, bb_w = 650, bb_h = 300, totalMissingSamples;
    double speed = 2.0;
    SignalProcessing signalProcessing;

    public void println(String text){
        System.out.println(text);
    }

    public void clearScreen(){
        gc.clearRect(0,0,surface.getWidth(),surface.getHeight());

        Image textImage = getImage(selectedTextID);
        gc.drawImage(textImage,0,0);
    }

    public void readImages(){
        imageFilenames = new HashMap<Integer, String>();
       try {
           FileReader fileReader = new FileReader("data/screen-shots/table.txt");
           BufferedReader bufferedReader = new BufferedReader(fileReader);
           String line;
           String splitLine[];
           while((line = bufferedReader.readLine()) != null){
               splitLine = line.split("\\s");
               imageFilenames.put(Integer.parseInt(splitLine[1]),splitLine[0]);
           }
       }
       catch (FileNotFoundException ex){
           println("Error reading the image");
       }
       catch (IOException ex){
           println("Error reading the image");
       }
    }

    public Image getImage(int textID){

        File imageFile = new File("data/screen-shots/" + imageFilenames.get(textID));
        Image img = new Image(imageFile.toURI().toString());
        return  img;

    }

    private boolean withinScreen(double x, double y){
        if( x < 0 || y < 0 || x > surface.getWidth() || y > surface.getHeight())
            return false;
        return  true;
    }

    public void initialize() {
        gc = surface.getGraphicsContext2D();
        randomNum = new Random();
        changeDataMode();
        readImages();
        setIdtDispersion();
        setIdtDuration();
        setSgolayLength();
        setSgolayOrder();

        ListView<String> list = new ListView<String>();
        File folder = new File("data/raw_data");
        File[] listOfFiles = folder.listFiles();
        String[] fileNames = new String[listOfFiles.length];
        for(int i = 0; i < listOfFiles.length; i++)
            fileNames[i] = listOfFiles[i].getName().substring(1,3);
        ObservableList<String> items = FXCollections.observableArrayList (fileNames);
        userList.setItems(items);
        ObservableList<Integer> textIDs = FXCollections.observableArrayList(11299,13165,5938,11143,322,10879,4584,4504,6366,4701,6474,4094,7784,6046,2725,3775,9977,3819);
        textList.setItems(textIDs);

        userList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                // Your action here
                if(mode == RAW) {
                    fileName = "data/split_raw_data/p" + newValue + "_" + textList.getSelectionModel().getSelectedItem() + "_ET.txt";
                    //label.setText(fileName);
                    selectedUserID = Integer.parseInt(newValue);
                }
                else if (mode == EVENTS){
                    fileName = "data/split_aggregated_data/p" + newValue + "_" + textList.getSelectionModel().getSelectedItem() + "_FS.txt";
                   // label.setText(fileName);
                   selectedUserID = Integer.parseInt(newValue);
                }
            }
        });

        textList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Integer>() {

            @Override
            public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
                // Your action here
                selectedTextID = newValue;
                if(mode == RAW) {
                    fileName = "data/split_raw_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + newValue + "_ET.txt";
                }
                else if (mode == EVENTS){
                    fileName = "data/split_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + newValue + "_FS.txt";
                }
            }
        });
        speed = speedSlider.getValue();
        speedSlider.valueProperty().addListener((obs, oldVal,newVal) ->
            speed = newVal.doubleValue());

        signalProcessing = new SignalProcessing();

        durationRadioButton.setSelected(true);
        userList.getSelectionModel().select(0);
        textList.getSelectionModel().select(0);
        durationDisplay = true;



    }

    private void readAndProcessRawData(){
        ETReader etReader = new ETReader();
        rawData = etReader.readETCollection(fileName);
        countOutliers();
        if (cleanOutliers)
            rawData = signalProcessing.cutOutliers(rawData,bb_x,bb_y,bb_w,bb_h);
        countMissingData();
        if (interpolation)
            rawData = signalProcessing.runInterpolation(rawData,minGapSize,maxGapSize);
        if(sgolay)
            rawData = signalProcessing.applySavitzkyGolayFilter(rawData,sgolayOrder,sgolayLength);
    }

    public void startButtonClick(){
        changeDataMode();
        isIDT = false;
        if(mode == RAW) {
            readAndProcessRawData();
        }
        else if(mode == EVENTS){
            FSReader fsReader = new FSReader();
            eventData = fsReader.readFSCollection(fileName);
        }
        startVisualization();
    }

    public boolean withinBoundingBox(double x, double y){
        return (x >= bb_x && y >= bb_y && x<= (bb_x + bb_w) && y <= (bb_y + bb_h));
    }

    public void startVisualization(){
        if(tm != null){
            tm.cancel();
            tm.purge();
        }
        tm = new Timer();
        index = 0;
        if(sgolay)index=1;
        clearScreen();
        gc.setStroke(Color.BLACK);
        gc.strokeRect(bb_x,bb_y,bb_w,bb_h);
        long interval = Math.round(20.0 / speed);
        if (isIDT){
            TimerTask task = new TimerTask() {
                public void run() {
                    if(durationLeft != 0) {
                        durationLeft--;
                        return;
                    }
                    if(index >= eventData.size){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    FSEvent event = eventData.events[index];

                    if(event.type == event.FIXATION){
                        //println(( Long.toString(event.duration)));
                        double disp = 1 + event.duration / 20000.0;
                        durationLeft = event.duration / 20000;
                        if(index != 0){
                            FSEvent prevEvent = eventData.events[index-1];
                            gc.setStroke(Color.BLACK);
                            gc.strokeLine( prevEvent.x, prevEvent.y, event.x, event.y);

                        }
                        if(event.x > 800)
                            gc.setStroke(Color.BLUE);
                        else if (event.x < 500)
                            gc.setStroke(Color.GREEN);
                        else
                            gc.setStroke(Color.BLACK);
                        if(durationDisplay) {
                            gc.strokeOval(event.x - disp / 2, event.y - disp / 2, disp, disp);
                        }
                        else{
                            gc.strokeOval(event.x - event.x2/2, event.y - event.y2/2, event.x2, event.y2);
                        }
                    }
                    else if(event.type == event.SACCADE){
                        gc.setStroke(Color.BLACK);
                        gc.strokeLine(event.x, event.y, event.x2, event.y2);
                    }
                    else{
                        println("Error reading event");
                    }
                    index++;
                }

            };
            tm.schedule(task,interval,interval);
        }
        else if(mode == RAW) {
            TimerTask task = new TimerTask() {
                public void run() {
                    if(index >= rawData.size){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    int w = 5, h = 5;
                    GazePoint pt = rawData.gazePoints[index];
                    double x = pt.x;
                    double y = pt.y;
                    index++;
                    if(withinBoundingBox(x,y))
                        gc.setStroke(Color.BLACK);
                    else
                        gc.setStroke(Color.RED);
                    gc.strokeOval(x, y, w, h);
                }

            };
            tm.schedule(task,interval,interval);
        }
        else if (mode == EVENTS){
            TimerTask task = new TimerTask() {
                public void run() {
                    if(durationLeft != 0) {
                        durationLeft--;
                        return;
                    }
                    if(index >= eventData.size){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    FSEvent event = eventData.events[index];
                    if(event.type == event.FIXATION){
                        //println(( Long.toString(event.duration)));
                        double disp = 1 + event.duration / 20000.0;
                        durationLeft = event.duration / 20000;
                        if(index != 0){
                            FSEvent prevEvent = eventData.events[index-1];
                            gc.strokeLine( prevEvent.x, prevEvent.y, event.x, event.y);

                        }
                        if(durationDisplay) {
                            gc.strokeOval(event.x - disp / 2, event.y - disp / 2, disp, disp);
                        }
                        else{
                            gc.strokeOval(event.x - event.x2/2, event.y - event.y2/2, event.x2, event.y2);
                        }
                    }
                    else if(event.type == event.SACCADE){
                        gc.strokeLine(event.x, event.y, event.x2, event.y2);
                    }
                    else{
                        println("Error reading event");
                    }
                    index++;
                }

            };
            tm.schedule(task,interval,interval);


        }

        else if(mode == TET){

            ETReader etReader =  new ETReader();
            rawData = etReader.readTETCollection(fileName);
            TimerTask task = new TimerTask() {
                public void run() {
                    if(index >= rawData.size){
                        tm.cancel();
                        tm.purge();
                        println("The end");
                        return;
                    }
                    int w = 5, h = 5;
                    GazePoint pt = rawData.gazePoints[index];
                    double x = pt.x;
                    double y = pt.y;
                    index++;
                    gc.strokeOval(x, y, w, h);
                }

            };
            tm.schedule(task,interval,interval);
        }
    }

    public void countMissingData(){
        totalMissingSamples = 0;
        long totalTime = 0;
        for (int i = 1; i < rawData.size; i++){
            GazePoint pt = rawData.gazePoints[i];
            GazePoint prevPt = rawData.gazePoints[i-1];
            int missingSamples = (int)(pt.timestamp - prevPt.timestamp) / 15000;
            if (missingSamples > minGapSize && missingSamples < maxGapSize)
                totalMissingSamples += missingSamples;

            if(missingSamples > minGapSize)
                totalTime += pt.timestamp - prevPt.timestamp;
        }
        timeMissingTextBox.setText(Double.toString(Math.round(totalTime / 1000.0)/1000.0));
        timeMissingSamplesTextBox.setText(Integer.toString(totalMissingSamples));
    }


    public void startNewForm() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("features.fxml"));
            FeatureController fc = new FeatureController();
            fc.receiveParameters(selectedUserID,selectedTextID,eventData,rawData);
            fxmlLoader.setController(fc);
            Parent root = fxmlLoader.load();
            Stage stage = new Stage();

            stage.setTitle("Feature Extraction Interface");
            stage.setScene(new Scene(root, 1000, 900));
            stage.setResizable(false);
            stage.show();
            System.out.printf("New form has been opened");
        }
        catch (Exception e){

            System.out.printf("New form could not be opened");
        }
    }

    private void extractFeatures() {

        startNewForm();

    }

    private void countOutliers(){
        int outliers = 0;
        for(GazePoint sample : rawData.gazePoints){
            if (!withinBoundingBox(sample.x, sample.y))
                outliers ++;
        }
        outliersTextBox.setText(Integer.toString(outliers));
        outliersProportionTextBox.setText(Double.toString(Math.round(outliers / (0.0001*rawData.size))/100.0));
    }


    public void calculateAllMeasures() throws IOException{

        ETReader etReader = new ETReader();
        FileWriter fileWriter = new FileWriter("measures.xls");
        String [] columns = {"person.ID","text.ID","Total Reading Time", "Scanpath Length", "Mean Pupil Dilation", "Pupillary Unrest Index", "Fixation Number", "Fixation Rate", "Mean Number Of Fixations Per Line","Number Of Line-to-line Saccades", "Number of Regressions", "Regression Rate", "Mean Number of Regressions Per Line",
                "Fixation Duration Mean","Fixation Duration SD","Fixation Duration Variance","Fixation Duration Skewness","Fixation Duration Kurtosis",
                "Fixation Dispersion Mean","Fixation Dispersion SD","Fixation Dispersion Variance","Fixation Dispersion Skewness","Fixation Dispersion Kurtosis",
                "Saccade Duration Mean","Saccade Duration SD","Saccade Duration Variance","Saccade Duration Skewness","Saccade Duration Kurtosis",
                "Saccade Amplitude Mean","Saccade Amplitude SD","Saccade Amplitude Variance","Saccade Amplitude Skewness","Saccade Amplitude Kurtosis",
                "Saccade Mean Velocity Mean","Saccade Mean Velocity SD","Saccade Mean Velocity Variance","Saccade Mean Velocity Skewness","Saccade Mean Velocity Kurtosis"};
        String headerLine = "User ID";
        for(int i = 1; i < columns.length; i++)
            headerLine += "\t" + columns[i];
        fileWriter.write(headerLine+"\n");
        for(int i = 0 ;  i < userList.getItems().size(); i++){
            double userFeatures[][] = new double[textList.getItems().size()][];
            String user_id = userList.getItems().get(i);
            int excluded = 0;
            boolean excluded_mask[] = new boolean[textList.getItems().size()];
            for(int j = 0; j < textList.getItems().size(); j++){
                String text_id = textList.getItems().get(j).toString();
                String fileName = "data/split_raw_data/p" + user_id + "_" + text_id + "_ET.txt";
                println("User ID: " + user_id + "\t Text ID: " + text_id);
                //if(user_id.compareTo("32") == 0)
                //{
                //    println("Hello");
                //}
                ETCollection etdata = etReader.readETCollection(fileName);
                if(etdata.size < 1000) {
                    excluded++;
                    excluded_mask[j] = true;
                    continue;
                }
                if(cleanOutliersCheckBox.isSelected())
                    etdata = signalProcessing.cutOutliers(etdata,bb_x,bb_y,bb_w,bb_h);
                if(interpolationCheckBox.isSelected())
                    etdata = signalProcessing.runInterpolation(etdata,minGapSize,maxGapSize);
                if(sgolayCheckBox.isSelected())
                    etdata = signalProcessing.applySavitzkyGolayFilter(etdata,sgolayOrder,sgolayLength);

                FSCollection fsdata = signalProcessing.runEventDetection(etdata,idtDispersion,idtDuration);
                if (fsdata == null) {
                    excluded++;
                    excluded_mask[j] = true;
                    continue;
                }
                double features[] = calculateAllMeasuresForAText(etdata,fsdata);
                userFeatures[j] = features;
                if(features[7] < 10) {
                    excluded++;
                    excluded_mask[j] = true;
                    continue;
                }
                saveEvents(fsdata,"data/saved_aggregated_data/p" + user_id + "_" + text_id + "_FS.txt",text_id);
                //double features[] = {1.0,2.0,3.0};
            }
            for(int k = 0; k < columns.length - 2; k++){
                double[]  distribution = new double [userFeatures.length-excluded];
                int w = 0;
                for(int j = 0; j < userFeatures.length; j++){
                    if(!excluded_mask[j]) {
                        distribution[w] = userFeatures[j][k];
                        w++;
                    }

                }
                double [] stats = calculateStatistics(distribution);
                double mean = stats[0], sd = stats[1];
                for(int j = 0; j < userFeatures.length; j++){
                    if(!excluded_mask[j])
                        userFeatures[j][k] = ( userFeatures[j][k] - mean) / sd;
                }

            }
            for(int j = 0; j < textList.getItems().size(); j++) {
                if( excluded_mask[j])
                    continue;
                fileWriter.write(user_id + "\t" + textList.getItems().get(j).toString());
                for (int k = 0; k < userFeatures[j].length; k++)
                    fileWriter.write("\t" + Double.toString(Math.round(userFeatures[j][k] * 100000000) / 100000000.0));
                fileWriter.write('\n');
            }
        }
        fileWriter.close();
    }

    private double[] calculateAllMeasuresForAText(ETCollection etdata, FSCollection fsdata){
        double featureVector[] =  new double[36];
        int sample_number = etdata.size;
        int fixation_number = fsdata.size;
        int saccade_number = fixation_number - 1;
        double fixation_durations[] = new double[fixation_number], fixation_dispersions[] = new double[fixation_number];
        double saccade_amplitudes[] = new double[saccade_number], saccade_durations[] = new double[saccade_number], saccade_mean_velocities[] = new double[saccade_number];
        double total_duration = 0, scanpath_length = 0;
        int j = 0, left_side_fixations = 0, right_side_fixations = 0, number_of_lines = 0, cd = 0,number_of_regressions = 0;
        for(int i = 0; i < fixation_number; i++){
            //Fixation features
            fixation_durations[i] = (fsdata.events[i].duration / 1000000.0);
            fixation_dispersions[i] = fsdata.events[i].x2;

            total_duration += fixation_durations[i];
            // Saccade features
            if(i!= fixation_number - 1){
                double x_dist = fsdata.events[i+1].x - fsdata.events[i].x, y_dist = fsdata.events[i+1].y  - fsdata.events[i].y;
                saccade_amplitudes[i] = Math.sqrt(x_dist*x_dist + y_dist*y_dist);
                scanpath_length += saccade_amplitudes[i];
                saccade_durations[i] = (fsdata.events[i+1].start - fsdata.events[i].end)/ 1000000.0;
                total_duration += saccade_durations[i];
                saccade_mean_velocities[i] = saccade_amplitudes[i] / saccade_durations[i];

                if(y_dist < -30 || (x_dist < -50 && y_dist < 10))
                    number_of_regressions ++;
            }

            if(fsdata.events[i].x > 800)
                right_side_fixations ++;
            else if(fsdata.events[i].x < 500)
                left_side_fixations ++;

            if(i > 5){
                if(cd > 0)
                    cd --;
                if(fsdata.events[i-6].x > 800)
                    right_side_fixations --;
                else if(fsdata.events[i-6].x < 500)
                    left_side_fixations --;
            }

            if(right_side_fixations >=3 && left_side_fixations >= 2){
                number_of_lines ++;
                cd = 5;
            }
        }
        double [] fixation_duration_statistics = calculateStatistics(fixation_durations);
        double [] fixation_dispersion_statistics = calculateStatistics(fixation_dispersions);
        double fixation_rate = fixation_number / total_duration;
        double fixation_per_line = fixation_number / (1.0* number_of_lines);

        double [] saccade_amplitude_statistics = calculateStatistics(saccade_amplitudes);
        double [] saccade_durations_statistics = calculateStatistics(saccade_durations);
        double [] saccade_mean_velocity_statistics = calculateStatistics(saccade_mean_velocities);

        double regression_rate = number_of_regressions / total_duration;
        double mean_number_of_regressions_per_line = number_of_regressions / (1.0* number_of_lines);

        // Pupillary
        int lastCount = sample_number % 16, curCount = 16;
        int numberOfBins = (sample_number + 15) / 16;
        if(lastCount == 0)
            lastCount = 16;
        double averageDilation[] = new double [numberOfBins];
        double curSum = 0;
        double totalDilationSum = 0;
        for(int i = 0; i < numberOfBins; i++) {
            curCount = (i == (numberOfBins - 1) ? lastCount : 16);
            curSum = 0;
            for (int k = 0; k < curCount; k++) {
                curSum += etdata.gazePoints[(i * 16) + k].pupil_diameter;
                totalDilationSum += etdata.gazePoints[(i * 16) + k].pupil_diameter;
            }
            averageDilation[i] = curSum / curCount;
        }

        double meanPupilDilation = totalDilationSum / sample_number;

        double differenceSum = 0;
        for(int i = 1; i < numberOfBins; i++){
            differenceSum += Math.abs(averageDilation[i] - averageDilation[i-1]);
        }
        double pui = 1/((sample_number - 16)*(1/60.0)) * differenceSum;


        featureVector[0] = total_duration;
        featureVector[1] = scanpath_length;
        featureVector[2] = meanPupilDilation;
        featureVector[3] = pui;
        featureVector[4] = fixation_number;
        featureVector[5] = fixation_rate;
        featureVector[6] = fixation_per_line;
        featureVector[7] = number_of_lines;
        featureVector[8] = number_of_regressions;
        featureVector[9] = regression_rate;
        featureVector[10] = mean_number_of_regressions_per_line;



        for(int i = 0; i < 5; i++)
            featureVector[11 + i] = fixation_duration_statistics[i];
        for(int i = 0; i < 5; i++)
            featureVector[16 + i] = fixation_dispersion_statistics[i];
        for(int i = 0; i < 5; i++)
            featureVector[21 + i] = saccade_durations_statistics[i];
        for(int i = 0; i < 5; i++)
            featureVector[26 + i] = saccade_amplitude_statistics[i];
        for(int i = 0; i < 5; i++)
            featureVector[31 + i] = saccade_mean_velocity_statistics[i];

        return featureVector;
    }

    private double[] calculateStatistics(double [] distribution){
        int n  =  distribution.length;
        double dn = 1.0*n;
        double sum = 0;
        for(int i = 0; i < n; i++)
            sum += distribution[i];
        double mean = sum / dn;
        double sum2 = 0, sum3 = 0, sum4 = 0, diff;
        for (int i = 0; i < n; i++){
             diff = (distribution[i] - mean);
             sum2 += diff*diff;
             sum3 += diff*diff*diff;
             sum4 += diff*diff*diff*diff;
        }
        double var = sum2 / dn;
        double sd = Math.sqrt(var);
        double skewness = sum3 / Math.pow(sum2, 1.5);
        double kurtosis = sum4 / (sum2*sum2);
        double result[] = {mean, sd, var, skewness, kurtosis};
        return result;
    }

    private void saveEvents(FSCollection eventData, String fileName, String text_id){
        try {
            FileWriter fw = new FileWriter(fileName);
            fw.write("rails_session_id\teye\tstart_event_name\tstart_event_stimulus_id\tstop_event_name\tstop_event_stimulus_id\tevent type\tnumber\tstart\tend\tduration\tlocation X\tlocation Y\tdispersion X\tdispersion Y\tavg. pupil size X\tavg. pupil size Y\n");
            for(int i = 0; i < eventData.events.length; i++){
               //String line;
                FSEvent event = eventData.events[i];
                String line = "1\tright\tstimulus_" + text_id + "\t" + text_id + "\tstimulus_" + text_id + "\t" + text_id + "\tFixation\tR\t" + (i+1) + "\t";
                fw.write(line);
                line = event.start + "\t" + event.end + "\t" + event.duration + "\t" + event.x + "\t" + event.y + "\t" + event.x2 + "\t" + event.y2 + "\t" + event.meanPupilDiameter + "\t" + event.meanPupilDiameter + "\n";
                fw.write(line);
            }
            fw.close();
        }
        catch (Exception ex) {
            println("Error writing to " + fileName);
            return;
        }
   }

    //////////////////////////////////////////////////////////////////////////////
    //////////////////////////// UI Functions ////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public void runEventDetection(){
        gazePointsRadioButton.setSelected(true);
        changeDataMode();

        readAndProcessRawData();
        eventData = signalProcessing.runEventDetection(rawData,idtDispersion,idtDuration);
        isIDT = true;
        extractFeatures();
        startVisualization();
    }


    public void changeDataMode(){
        if(gazePointsRadioButton.isSelected()) {
            mode = RAW;
            fileName = "data/split_raw_data/p" + userList.getSelectionModel().getSelectedItem()+ "_" + textList.getSelectionModel().getSelectedItem() + "_ET.txt";

        }
        else if(eventsRadioButton.isSelected()) {
            mode = EVENTS;
            fileName = "data/split_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + textList.getSelectionModel().getSelectedItem() + "_FS.txt";
        }
        else if(savedEventsRadioButton.isSelected()) {
            mode = EVENTS;
            fileName = "data/saved_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + textList.getSelectionModel().getSelectedItem() + "_FS.txt";
        }
    }

    public void changeEventVisualizationMode(){
        if(durationRadioButton.isSelected()) {
            durationDisplay = true;
        }
        else if(dispersionRadioButton.isSelected()){
            durationDisplay = false;
        }
    }

    public void setCleanOutliers(){
        cleanOutliers = cleanOutliersCheckBox.isSelected();
    }

    public void setIdtDispersion(){
        try{
            idtDispersion = Integer.parseInt(dispersionTextBox.getText());
        }
        catch (NumberFormatException ex){
            dispersionTextBox.setText("Enter integer");
        }
    }

    public void setIdtDuration(){
        try{
            idtDuration = Integer.parseInt(durationTextBox.getText());
        }
        catch (NumberFormatException ex){
            durationTextBox.setText("Enter integer");
        }
    }

    public void setInterpolation(){
        interpolation = interpolationCheckBox.isSelected();
    }

    public void setSgolay(){
        sgolay = sgolayCheckBox.isSelected();
    }


    public void setMinGapSize(){
        try{
            minGapSize = Integer.parseInt(minGapSizeTextBox.getText());
        }
        catch (NumberFormatException ex){
            if(!minGapSizeTextBox.getText().isEmpty())
                minGapSizeTextBox.setText("Enter integer");
        }
    }

    public void setMaxGapSize(){
        try{
            maxGapSize = Integer.parseInt(maxGapSizeTextBox.getText());
        }
        catch (NumberFormatException ex){
            if(!minGapSizeTextBox.getText().isEmpty())
                minGapSizeTextBox.setText("Enter integer");
        }
    }


    public void setSgolayOrder(){
        try{
            sgolayOrder = Integer.parseInt(sgolayOrderTextBox.getText());
        }
        catch (NumberFormatException ex){
            if(!sgolayOrderTextBox.getText().isEmpty())
                sgolayOrderTextBox.setText("Enter integer");
        }
    }

    public void setSgolayLength(){
        try{
            if(Integer.parseInt(sgolayLengthTextBox.getText()) % 2 == 1)
                sgolayLength = Integer.parseInt(sgolayLengthTextBox.getText());
            else
                sgolayLengthTextBox.setText("Enter an odd integer");
        }
        catch (NumberFormatException ex){
            if(!sgolayLengthTextBox.getText().isEmpty())
                sgolayLengthTextBox.setText("Enter integer");
        }
    }


    public void stop(){
        if(tm!=null) {
            tm.cancel();
            tm.purge();
        }
    }

}
