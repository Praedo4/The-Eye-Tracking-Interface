package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.rosuda.JRI.*;

import java.util.Enumeration;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("The Eye-Trcaking Interface");
        primaryStage.setScene(new Scene(root, 1340, 1150));
        primaryStage.show();
    }


    public static void main(String[] args) {
        //Rengine re=new Rengine(args, false, new TextConsole());
        //re.eval("print(1:10/3)");
        //System.out.println(x=re.eval("iris"));
        //re.eval("source(\"D:\\\\Coding\\\\Mockup\\\\r\\\\rtest1.R\")");
        //REXP result;

        //re.assign("x",result);
        //re.assign("x", "Hello");
       // result = re.eval("fixations");
        //System.out.print(result.asString());
       //System.out.println("Result: "+result.asString());
        //REXP x = re.eval("greeting ");
        //System.out.println("Greeting R: "+x.asString());
       //RList v = x.asList();
       //String[] l = v.keys();
       //for(String item : l){
       //    System.out.println(item);;
       //}
       // String s = x.asString();
        //System.out.println(s);
        //if (v.getNames()!=null) {
        //    System.out.println("has names:");
        //    for (Enumeration e = v.getNames().elements(); e.hasMoreElements() ;) {
        //        System.out.println(e.nextElement());
        //    }
        //}
        launch(args);
    }
}
