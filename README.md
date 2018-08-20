# The-Eye-Tracking-Interface
Master thesis project

<< :  Mockup :  >>

The framework with GUI for data visualization, signal processing and feature extraction

To run project follow these steps:
1. Make sure you have JDK8 (64-bit) installed: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
2. Make sure you have R 3.4.3 (64-bit) installed: https://cran.r-project.org/bin/windows/base/old/3.4.3/
3. For the program to run, JRI is required. It is automatically installed when rJava is installed in R. To do that, we open R 3.4.3 (or RStudio if it runs that version of R) and run the command << install.packages('rJava') >>. This should install both rJava and JRI. If promt asks for usage of local directory (for user) - confirm.
4. To build a project, a java IDE is required. The one that was used when developing the project is 'IntelliJ IDEA Community Edition 2017.3.3', but any IDE can be used. However, following steps will refer to IntelliJ, for others refer to the same settings within the IDE.
5. Set up the proper Java version (8) for the project.
> Note: in case of following compilation error: 'java: invalid source release: 9', in IntelliJ proceed to 'File -> Project structure -> Project' in IntelliJ and set 'Project language level' to 8 and rebuild the project afterwards.
6. Go to run/debug configurations (in IntelliJ: Run -> Edit configurations -> Application -> Main) and set 'VM options' to be << -Djava.library.path="C:\Users\xxxxx\Documents\R\win-library\3.4\rJava\jri\x64" >>, where xxxxx is the username for which the R library was installed. Check the path to lead to the rJava\jri folder. This path might differ depending on the system. This example presents Windows 10 path.
7. In the run/debug configurations, set environment variable 'PATH' to be 'C:/Program Files/R/R-3.4.3/bin/x64'. This should be already done in the IntelliJ project file, that is already present for the project on the GitHub.
8. Build and run the program. First time that 'Run I-DT' button is pressed, there will be a promt message in case 'emov' package is required to be installed. In this case it will launch the installation an R package. Close the application and run it again for it to work correctly. Installation process is extremely inconsistent to control, but it should be promted when it's done.

After all of this, hopefully the project is up and running!

Input data can be selected from: raw data, aggregated data reported from the eye tracker (for comparison only) and the aggregated data saved after processing.

Different signal processing methods can be enabled and disabled with respective checkboxes, as well as changing parameters used for the processing.

Using 'Calculate measures' button runs the whole framework with chosen processing steps for all raw data files and calculates measures from the eye tracking data. This also saves the event data into respective folder, that can be acccessed later on using the 'saved' input data option.

<< : KS and CLT tests : >>

These statistical tests were implemented in python with solutions commented in-line. Tests can be run using original data and processed data by changing the path within the code. For any questions, contact at praedo4421@gmail.com.

<< : Interest modelling : >>

Machine learning models that are used for predicting interest from the measures obtained from the framework. Implemented in R, using latest version (3.5.1 at the time of release). In order to run the project, install the latest version of R and all the dependencies, as listed in the beggining of the source file. A function 'install_packages' is defined in the begging of the source file, that should update R and install all the packages. In case that does not work, install the packages manually using the same list, as provided in the function.

The script can be split into three distinct sections: reading the input data, defining the test functions and running the tests. In the first section, there are several parameters that are user-defined and are then used for further processing. In case these parameters are changed, the whole solutions needs to be recompiled. These parameters are: input_file ('' or 'normalized'), interesting_threshold (values 3.5, 4 and 4.5 were tested extensively) and cor_threshold (0.3 and 0.4 are recommended).  The test functions are defined for all three cases: classification, regression and classification combined with regression. Due to specific implementation of feature selection methods from package FSelector, the test function has to take only one argument, that is the feature subset that is being interated. Therefore, all the parameters are defined in a separate array 'parameters' and are then manipulated when expensive testing is done in the third section of the program. Due to implementation limitations of namespace variable visibility, three testing routines are NOT defined in separate functions, but are split into three blocks separated with comments in line. These three blocks are meant to be run at separate occasions depending on the specific set of parameters that are being tested. Before each sections, where the tests are run, boolean values are defined for each parameter. True values means that this parameter is tested for all the values defined in the respective array, while False values means that the default (the first) value is used for the parameter. Directory names and filenames are constructed based on the set of parameters. Note that directories for test results have to be created prior to the test to be populated, and folders are not created procedurally. 
