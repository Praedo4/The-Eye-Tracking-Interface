package sample;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import org.rosuda.JRI.*;
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
    @FXML private RadioButton gazePointsRadioButton;
    @FXML private RadioButton tetRadioButton;
    @FXML private RadioButton dispersionRadioButton;
    @FXML private RadioButton durationRadioButton;
    @FXML private CheckBox cleanCheckBox;
    @FXML private ListView<String> userList;
    @FXML private ListView<Integer> textList;
    @FXML private Label visualizationLabel;
    @FXML private TextField dispersionTextBox;
    @FXML private TextField durationTextBox;
    @FXML private TextField minGapSizeTextBox;
    @FXML private TextField maxGapSizeTextBox;
    @FXML private TextField sgolayOrderTextBox;
    @FXML private TextField sgolayLengthTextBox;
    @FXML private TextField timeMissingTextBox;
    @FXML private TextField timeMissingSamplesTextBox;
    @FXML private TextField lineSaccadesTextBox;
    @FXML private CheckBox interpolationCheckBox;
    @FXML private CheckBox sgolayCheckBox;
    @FXML private Slider speedSlider;
    private GraphicsContext gc;
    private Random randomNum;
    private Timer tm;
    Rengine re;
    //private FileReader fr;
    //private BufferedReader br;
    int index = 0;
    String fileName = "data/raw_data/p01_ET_samples.txt";
    private ETCollection rawData;
    private FSCollection eventData;
    int mode;
    boolean clean, durationDisplay = false;
    final int RAW = 0, EVENTS = 1, TET = 2;
    int selectedTextID = -1, selectedUserID = -1;
    long durationLeft = 0;
    int idtDispersion, idtDuration;
    boolean isIDT = false;
    Map<Integer, String> imageFilenames;
    int minGapSize = 3, maxGapSize = 10; boolean interpolation = false, sgolay = false; int sgolayOrder = 1, sgolayLength = 25;
    double speed = 2.0;

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
        //label.setText(fileName);
        gc = surface.getGraphicsContext2D();
        randomNum = new Random();
        changeDataMode();
        setClean();
        readImages();
        setIdtDispersion();
        setIdtDuration();

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
                    //label.setText(fileName);
                }
                else if (mode == EVENTS){
                    fileName = "data/split_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + newValue + "_FS.txt";
                    //
                    //
                    // label.setText(fileName);
                }
            }
        });
        speed = speedSlider.getValue();
        speedSlider.valueProperty().addListener((obs, oldVal,newVal) ->
            speed = newVal.doubleValue());

        String args[] = null;
        re=new Rengine(args, false, new TextConsole());
        re.eval("require(emov)");
        durationRadioButton.setSelected(true);
        userList.getSelectionModel().select(0);
        textList.getSelectionModel().select(0);
        durationDisplay = true;


    }

    public void startButtonClick(){
        changeDataMode();
        setClean();
        isIDT = false;
        startVisualization();
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
                    if(clean && !withinScreen(event.x,event.y)){
                        index++;
                        return;
                    }
                    if(event.type == event.FIXATION){
                        //println(( Long.toString(event.duration)));
                        double disp = 1 + event.duration / 20000.0;
                        durationLeft = event.duration / 20000;
                        if(index != 0){
                            FSEvent prevEvent = eventData.events[index-1];
                            if(!clean || (withinScreen(event.x,event.y) && withinScreen(prevEvent.x, prevEvent.y)))
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
                        if(clean && !withinScreen(event.x2,event.y2)){
                            index++;
                            return;
                        }
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
            ETReader etReader = new ETReader();
            rawData = etReader.readETCollection(fileName);
            if(interpolation)
                runInterpolation();
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
                    if(clean && !withinScreen(x,y))
                        return;
                    gc.strokeOval(x, y, w, h);
                }

            };
            tm.schedule(task,interval,interval);
        }
        else if (mode == EVENTS){
            FSReader fsReader = new FSReader();
            eventData = fsReader.readFSCollection(fileName);
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
                    if(clean && !withinScreen(event.x,event.y)){
                        index++;
                        return;
                    }
                    if(event.type == event.FIXATION){
                        //println(( Long.toString(event.duration)));
                        double disp = 1 + event.duration / 20000.0;
                        durationLeft = event.duration / 20000;
                        if(index != 0){
                            FSEvent prevEvent = eventData.events[index-1];
                            if(!clean || (withinScreen(event.x,event.y) && withinScreen(prevEvent.x, prevEvent.y)))
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
                        if(clean && !withinScreen(event.x2,event.y2)){
                            index++;
                            return;
                        }
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
                    if(clean && !withinScreen(x,y))
                        return;
                    gc.strokeOval(x, y, w, h);
                }

            };
            tm.schedule(task,interval,interval);
        }
    }

    public void runInterpolation(){
        int totalMissingSamples = 0;
        long totalTime = 0;
        for (int i = 1; i < rawData.size; i++){
            GazePoint pt = rawData.gazePoints[i];
            GazePoint prevPt = rawData.gazePoints[i-1];
            int missingSamples = (int)(pt.timestamp - prevPt.timestamp) / 15000;
            if (missingSamples > minGapSize && missingSamples < maxGapSize){
                //List<> newRawData
                //for(int j = 0; j < missingSamples; j ++){
                //    rawData.gazePoints.
                //}
                println("Missing samples: " + missingSamples);
                totalMissingSamples += missingSamples;
            }
            if(missingSamples > minGapSize)
                totalTime += pt.timestamp - prevPt.timestamp;
        }
        println("Total missing samples: " + totalMissingSamples);
        timeMissingTextBox.setText(Long.toString(totalTime / 1000));
        timeMissingSamplesTextBox.setText(Integer.toString(totalMissingSamples));
        GazePoint[] newRawData = new GazePoint[rawData.size + totalMissingSamples];
        int missingSamplesAdded  = 0;
        newRawData[0] = rawData.gazePoints[0];
        for (int i = 1; i < rawData.size; i++){
            GazePoint pt = rawData.gazePoints[i];
            GazePoint prevPt = rawData.gazePoints[i-1];
            int missingSamples = (int)(pt.timestamp - prevPt.timestamp) / 15000;
            if (missingSamples > minGapSize && missingSamples < maxGapSize){
                for(int j = 1; j <= missingSamples; j++){
                    double newX = prevPt.x + (pt.x - prevPt.x) * (j * 1.0 / missingSamples);
                    double newY = prevPt.y + (pt.y - prevPt.y) * (j * 1.0 / missingSamples);
                    double newDiameter = prevPt.pupil_diameter + (pt.pupil_diameter - prevPt.pupil_diameter) * (j * 1.0 / missingSamples);
                    long newTimestamp = prevPt.timestamp + 15000 * j;
                    long newDuration = 15000;
                    GazePoint newPt = new GazePoint(newX,newY,newTimestamp,newDuration,newDiameter);
                    newRawData[i + missingSamplesAdded] = newPt;
                    missingSamplesAdded++;
                    println("Added #"+missingSamplesAdded + "at (" + newX + "," + newY + ") ts:" + newTimestamp + " duration: " + newDuration);
                }
            }
            newRawData[i + missingSamplesAdded] = rawData.gazePoints[i];
        }
        rawData.initialize(newRawData);
        println("Interpolation complete");


    }

    public void runEventDetection(){
        // Create lists of timestamps, xs and ys
        //if(mode != RAW){
            gazePointsRadioButton.setSelected(true);
            changeDataMode();
            setClean();
            ETReader etReader = new ETReader();
            rawData = etReader.readETCollection(fileName);
            if(interpolation)
                runInterpolation();
        //}

        int n = rawData.size;
        int [] timestamps = new int [n];
        long timeStampStart;
        double xs[] = new double[n],ys[] = new double[n];
        timeStampStart = rawData.gazePoints[0].timestamp;
        for(int i = 0 ; i < n-6; i++){
            GazePoint pt = rawData.gazePoints[i];
            timestamps[i] = (int) (pt.timestamp - timeStampStart);
            xs[i] = pt.x;
            ys[i] = pt.y;
        }

        REXP rResult;
        // Pass the data to R
        if(re.assign("ts",timestamps) && re.assign("xs",xs) && re.assign("ys", ys)){
            // Run IDT
            if(sgolay) { // Run Savitzky-Golay smoothing filter
                rResult = re.eval("library(signal, warn.conflicts = FALSE)");
                //Result = re.eval("");
                //re.assign("sg1",rResult);
                rResult = re.eval("print(xs)");
                //rResult = re.eval("print(sg1)");
                rResult = re.eval("filter(sgolay(p=" + sgolayOrder + ", n=" + sgolayLength + ", m=0), xs)");
                if (rResult == null) {
                    re.eval("install.packages(\'signal\')");
                    rResult = re.eval("require(signal)");
                    rResult = re.eval("filter(sgolay(p=1, n=25, m=0), xs)");
                }
                re.assign("xs", rResult);
                rResult = re.eval("filter(sgolay(p=1, n=25, m=0), ys)");
                re.assign("ys", rResult);
            }
            String callIDT = "emov.idt(ts,xs,ys," + idtDispersion + "," + idtDuration + ")";
            rResult = re.eval(callIDT);
            if(rResult == null){
                println("rJava and/or emov package are missing. Launching installation process");
                re.eval("install.packages(\'rJava\')");
                re.eval("install.packages(\'emov\')");
                rResult = re.eval(callIDT);
            }
            // Get the data back from R
            RList l = rResult.asList();
            int starts[] = l.at(0).asIntArray();
            int ends[] = l.at(1).asIntArray();
            int durs[] = l.at(2).asIntArray();
            double out_xs[] = l.at(3).asDoubleArray();
            double out_ys[] = l.at(4).asDoubleArray();
            // Parse the data
            int numberOfFixations = starts.length;
            FSEvent[] fsData = new FSEvent[numberOfFixations];
            for(int i = 0; i < numberOfFixations; i++){
                System.out.printf("Fixation #%d: starts at %d ends at %d (duration: %d) at position (%f, %f)\n",i+1,starts[i],ends[i],durs[i],out_xs[i],out_ys[i]);
                eventData = new FSCollection();
                fsData[i] = new FSEvent(out_xs[i],out_ys[i],1,1,timeStampStart + starts[i], timeStampStart + ends[i], durs[i], true);
            }
            eventData.initialize(fsData);
            // Visualize the data
            isIDT = true;
            //showEventVisualizationControls(1);
            extractFeatures();
            startVisualization();
        }
        else{
            println("Failed to assign data to R");
        }

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
            System.out.printf("yay");
        }
        catch (Exception e){

            System.out.printf("nay");
        }
    }

    private void extractFeatures() {

        //int lineSaccades = 0;
        //for(int i  = 1 ; i < eventData.size; i ++){
        //    double xdist = eventData.events[i].x - eventData.events[i-1].x;
        //    double ydist = eventData.events[i].y - eventData.events[i-1].y;
        //    double distSq = xdist*xdist + ydist*ydist;
        //    if (distSq > 100000 && ydist / xdist < 0.03)
        //        lineSaccades++;
        //}
        //lineSaccadesTextBox.setText(Integer.toString(lineSaccades));
        startNewForm();

    }

    private void showEventVisualizationControls(int op){
        dispersionRadioButton.setOpacity(op);
        durationRadioButton.setOpacity(op);
        cleanCheckBox.setOpacity(op);
        visualizationLabel.setOpacity(op);

    }

    public void changeDataMode(){
        if(gazePointsRadioButton.isSelected()) {
            mode = RAW;
            fileName = "data/split_raw_data/p" + userList.getSelectionModel().getSelectedItem()+ "_" + textList.getSelectionModel().getSelectedItem() + "_ET.txt";
           // label.setText(fileName);
           //showEventVisualizationControls(0);

        }
        else if(eventsRadioButton.isSelected()) {
            mode = EVENTS;
            fileName = "data/split_aggregated_data/p" + userList.getSelectionModel().getSelectedItem() + "_" + textList.getSelectionModel().getSelectedItem() + "_FS.txt";
           // label.setText(fileName);
           /// showEventVisualizationControls(1);
        }
        else if(tetRadioButton.isSelected()) {
            mode = TET;
            fileName = "data/tet/TET_raw_" + userList.getSelectionModel().getSelectedItem() + ".txt";
           // label.setText(fileName);
           // showEventVisualizationControls(0);
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

    public void setClean(){
        clean = cleanCheckBox.isSelected();
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

    public void chooseFile(){
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setInitialDirectory(new File("data/.").getAbsoluteFile());
        File file = fileChooser.showOpenDialog(surface.getScene().getWindow());
        try{
            if(file == null)
                return;
            fileName = file.getCanonicalPath();
            if(tm!=null) {
                tm.cancel();
                tm.purge();
            }
            selectedTextID = -1;
            initialize();
        }
        catch(IOException ex){
            println("File not found");
        }
    }

    public void stop(){
        if(tm!=null) {
            tm.cancel();
            tm.purge();
        }
    }



    //EventHandler<ActionEvent> buttonHandler = new EventHandler<ActionEvent>() {
    //    @FXML
    //    public void handle(ActionEvent event) {
    //        statusLabel.setText("Accepted");
    //        event.consume();
    //    }
    //};

}
