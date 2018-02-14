#################################
#### Require
#################################
.libPaths('C:\\Users\\John Doe\\Documents\\R')
library('emov')
library('randomForest')
#Sys.setenv(JAVA_HOME='C:\\Program Files\\Java\\jdk1.8.0_131')
library('rJava')
print("1");
library('xlsxjars')
print("2");
library('xlsx')
print("3");

#################################
#### READ RAW DATA
#################################

read_iviewsamples_fillmissing <- function(file) {
  #Properly imports SMI iview samples that has missing data. Do not use emov:read_iviewsamples!
    #File name is in the format of : "../data/p01_ET_samples.txt"
  return(read.table(file, header=TRUE, skip=0, sep="\t", fill=TRUE))
}

printf <- function(...) cat(sprintf(...))

#Create a list of filenames
files <- list.files("data/", pattern="p*_ET_samples.txt", full.names=TRUE)
listNames <- make.names(gsub("*_ET_samples.txt$", "", gsub("data/", "", files)))
listSplitNames <- paste(listNames, "_split", sep="")

#Load raw samples
#Warning: this takes a while.
printf("LOADING RAW SAMPLES\n")
list2env(
  lapply(setNames(files, listNames),
         read_iviewsamples_fillmissing), envir = .GlobalEnv)

#Split by text id
raw_split <- function(x) {
  dataframe <- as.data.frame(get(x))
  dataframe <- split(dataframe, dataframe$stimulus_id)
  #Remove non-text (invalid) elements in raw data
  dataframe['0'] <- NULL
  dataframe['1'] <- NULL
  dataframe['2'] <- NULL
  return(dataframe)
}

#Split raw samples by text id
#Warning: this takes a while
printf("SPLITTING RAW SAMPLES\n")
list2env(
  lapply(setNames(listNames, listSplitNames),
         raw_split), envir = .GlobalEnv)

#Clean up non-split samples
rm(list = listNames)


#Load participant ratings
#library(xlsx) #If you don't have this package run the -- install.packages("xlsx") -- command
participant_ratings <- read.xlsx("data/participant_ratings.xlsx", sheetName="user ratings")
participant_ratings <- participant_ratings[,0:6] #Drop not-used columns


printf("LOADING SUCCESSFUL\n")

##################################################
### SIGNAL PROCESSING & FEATURE EXTRACTION
##################################################

#Create data-frame for text features
text_features <- data.frame()

#Processes raw data and returns a dataframe with feature parameters
process_raw_data <- function(dispersion, min_duration, max_velocity) {
  user_names <- listSplitNames
  all_features <- lapply(user_names, process_texts, dispersion = dispersion, min_duration = min_duration, max_velocity = max_velocity)
  return(do.call("rbind", all_features))
}

process_texts <- function(x, dispersion, min_duration, max_velocity) {

  user_data <- get(x)
  text_names <- names(user_data)

  #GET USER ID
  user_id <- gsub("p*_split", "", x)
  user_id <- substring(user_id, 2)
  user_id <- as.integer(user_id)

  printf("************************************\n")
  printf("*** Processing texts for user %i ***\n", user_id)
  printf("************************************\n")

  records <- lapply(text_names, process_text, user_data_name = x, dispersion = dispersion, min_duration = min_duration, max_velocity = max_velocity)


  return(do.call("rbind", records))
}

sg <- sgolay(p=1, n=13, m=0)

process_text <- function(text_name, user_data_name, dispersion, min_duration, max_velocity)
{
  #GET DATAFRAME
  dataframe <- get(user_data_name)[[text_name]]

  #CALCULATE USER ID
  user_id <- gsub("p*_split", "", user_data_name)
  user_id <- substring(user_id, 2)
  user_id <- as.integer(user_id)

  #CALCULATE TEXT ID
  text_id <- as.integer(text_name)

  printf("*** Processing text id: %i ***\n", text_id)

  #Create a record
  features <- data.frame(user_id=integer(), text_id=integer(),
                         fixationsOverall=integer(), veryLongFixations=integer(), longFixations=integer(), shortFixations=integer(), fixationDurationMeanOverall=double(), fixationRateOverall=double(), fixationDurationTotal=double(), fixationDurationMedian=double(),
                         fixationsBegin=integer(), fixationsMid=integer(), fixationsEnd=integer(),
                         fixationDurationMeanBegin=double(),  fixationDurationMeanMiddle=double(), fixationDurationMeanEnd=double(),
                         fixationDurationTotalBegin=double(), fixationDurationTotalMiddle=double(), fixationDurationTotalEnd=double(),
                         gazePercentageBegin=double(), gazePercentageMiddle=double(), gazePercentageEnd=double(),
                         backtrackSaccades=integer(), saccadesOverall=integer(),
                         stringsAsFactors=FALSE)

  #Name records
  names(features) <- c("person.ID", "text.ID",
                        "fixationsOverall", "veryLongFixationsOverall", "longFixationsOverall", "shortFixationsOverall", "fixationDurationMeanOverall", "fixationRateOverall", "fixationDurationTotal", "fixationDurationMedian",
                        "fixationsBegin", "fixationsMiddle", "fixationsEnd",
                        "fixationDurationMeanBegin", "fixationDurationMeanMiddle", "fixationDurationMeanEnd",
                        "fixationDurationTotalBegin", "fixationDurationTotalMiddle", "fixationDurationTotalEnd",
                        "gazePercentageBegin", "gazePercentageMiddle", "gazePercentageEnd",
                        "backtrackSaccadesOverall", "saccadesOverall")

  #####################################
  #  SIGNAL PROCESSING
  #####################################

  ##### Low-pass filter
  #####
  dataframe$L.POR.X..px. = stats::filter(dataframe$L.POR.X..px., rep(1/3, 3))
  dataframe$L.POR.Y..px. = stats::filter(dataframe$L.POR.Y..px., rep(1/3, 3))

  #View(hampel(dataframe$L.POR.X..px., k=20))
  #dataframe$L.POR.X..px. = filter(sg, dataframe$L.POR.X..px.)
 # dataframe$L.POR.Y..px. = filter(sg, dataframe$L.POR.Y..px.)

  #Count (filtered) gaze samples
  noGazeSamples <- nrow(dataframe)
  firstFrame <- head(dataframe$Time, n=1L)

  #Convert time from microseconds to seconds
  dataframe$Time <- (dataframe$Time - firstFrame) / 1000 / 1000

  #### I-DT algorithm
  ####
  #Calculate fixations with I-DT algorithm
  #Note: dispersion is measured in PIXELS and time in SECONDS
  fixations <- emov.idt(dataframe$Time, dataframe$L.POR.X..px., dataframe$L.POR.Y..px., dispersion, min_duration)
  drops <- c("x","y")

  #### Velocity threshold filter
  ####
  #Apply velocity threshold filter
  fixations <- cbind(fixations[ , !(names(fixations) %in% drops)], emov.filter(fixations$x, fixations$y, max_velocity))
  fixations <- fixations[!is.na(fixations$x),] #Drop N/A (erronous) data

 # print(rownames(fixations))
  #View(fixations)
  ans1 <- !((fixations[-1,]$y > fixations[-nrow(fixations),]$y - dispersion / 2) & (fixations[-1,]$y < fixations[-nrow(fixations),]$y + dispersion / 2)) & (fixations[-1,]$y <= fixations[-nrow(fixations),]$y + dispersion / 2)
  noBacktrackSaccades <- sum(ans1)
  print(noBacktrackSaccades)
#  plot(x=NULL, y=NULL, xlim=c(0, 1280), ylim=c(1024, 0)) #Create new screen graph, don't forget to set ylim to 1024,0 instead of 0,1024 otherwise you get a reversed image.
#  points(fixations$x, fixations$y) #Plot points for text w/ id=`2725` on created graph.

  ### WE CAN'T JUST DROP <0 stuff because some text entries are entirely in negative y-space
  #Drop invalid fixations with y < 0 or y > 1024
  #fixations <- fixations[fixations$y %in% fixations$y[fixations$y>0],]
  #fixations <- fixations[fixations$y %in% fixations$y[fixations$y<1024],]

  #Get physical size of text from lowest and highest y values in fixations
  #Calculate lowest and highest y values
  #maxY <- max(fixations$y, na.rm = TRUE)
  #minY <- min(fixations$y, na.rm = TRUE)
  #sizeY <- (maxY - minY) / 3   #Size in y-space of an AOI

  medianY <- median(fixations$y, na.rm = TRUE)
 # print(medianY)
  stdev <- sd(fixations$y, na.rm = TRUE)
#  print(stdev)

#  abline(h = medianY, col = "gray60")

  sizeY <- stdev + 50
  minY <- medianY - sizeY
  maxY <- medianY + sizeY
  sizeY <- (maxY - minY) / 3

 # abline(h = medianY, col = "red")
 # abline(h = minY, col = "red")
 # abline(h = maxY, col = "red")

 # print(maxY)
#  print(minY)


  #####################################
  #  FEATURE PARAMETERS
  #####################################
  noValidGazeSamples <- nrow(dataframe[dataframe[, "L.POR.Y..px."]>(minY) & dataframe[, "L.POR.Y..px."]<(maxY),])
  totalDuration <- (tail(dataframe$Time, n=1L) - head(dataframe$Time, n=1L))

  #Calculate gaze-% per AOI
  gazeSamples_Begin <- dataframe[dataframe[, "L.POR.Y..px."]>(minY) & dataframe[, "L.POR.Y..px."]<(minY + sizeY),]
  gazeSamples_Begin <- gazeSamples_Begin[complete.cases(gazeSamples_Begin[ , "Time"]),] #Drop erronous data records
  noGazeSamples_Begin <- nrow(gazeSamples_Begin)
  percentGazeSamples_Begin <- noGazeSamples_Begin / noValidGazeSamples

  gazeSamples_Mid <- dataframe[dataframe[, "L.POR.Y..px."]>(minY + sizeY) & dataframe[, "L.POR.Y..px."]<(maxY - sizeY),]
  gazeSamples_Mid <- gazeSamples_Mid[complete.cases(gazeSamples_Mid[ , "Time"]),] #Drop erronous data records
  noGazeSamples_Mid <- nrow(gazeSamples_Mid)
  percentGazeSamples_Mid  <- noGazeSamples_Mid  / noValidGazeSamples

  gazeSamples_End <- dataframe[dataframe[, "L.POR.Y..px."]>(maxY - sizeY) & dataframe[, "L.POR.Y..px."]<(maxY),]
  gazeSamples_End <- gazeSamples_End[complete.cases(gazeSamples_End[ , "Time"]),] #Drop erronous data records
  noGazeSamples_End <- nrow(gazeSamples_End)
  percentGazeSamples_End  <- noGazeSamples_End  / noValidGazeSamples

  noFixationsOverall <- nrow(fixations)  #Calculate amount of fixations overall
  if (is.na(noFixationsOverall))
  {
    noFixationsOverall <- 0
  }


  meanFixationDuration <- mean(fixations$dur) #Calculate mean fixation duration overall
  printf("mean fixation duration: %f\n", meanFixationDuration)
  if (is.nan(meanFixationDuration))
  {
    meanFixationDuration <- 0.0
  }

  medianFixationDuration <- median(fixations$dur) #Calculate median fixation duration overall
  printf("median fixation duration: %f\n", medianFixationDuration)
  if (is.nan(medianFixationDuration))
  {
    medianFixationDuration <- 0.0
  }


  #Get first time instance and last time instance
  firstFrame <- head(fixations$start, n=1L)
  lastFrame <- tail(fixations$end, n=1L)

  totalDuration <- (lastFrame - firstFrame) #Calculate total fixation duration
  if (identical(totalDuration, numeric(0)))
  {
    totalDuration <- 0.0
  }

  fixationRateOverall <- noFixationsOverall / totalDuration #Calculate fixation rate overall
  if (identical(fixationRateOverall, numeric(0)))
  {
    totalDuration <- 0.0
  }
  if (is.nan(fixationRateOverall))
  {
    fixationRateOverall <- 0.0
  }

  longFixations <- nrow(
    fixations[fixations[,"dur"] > (0.25) & fixations[,"dur"] < (0.5),]
    )  #Calculate number of long fixations
  if (is.na(longFixations))
  {
    longFixations <- 0
  }

  veryLongFixations <- nrow(
    fixations[fixations[,"dur"] >= (0.5),]
  )  #Calculate number of long fixations
  if (is.na(veryLongFixations))
  {
    veryLongFixations <- 0
  }

  shortFixations <- nrow(fixations[fixations[,"dur"] < (0.2),])  #Calculate number of short fixations
  if (is.na(shortFixations))
  {
    shortFixations <- 0
  }

  #Fixations per AOI
  fixations_Begin <- fixations[fixations[, "y"]>=(minY) & fixations[, "y"]<(minY + sizeY),]
  noFixations_Begin <- nrow(fixations_Begin)
  if (is.na(noFixations_Begin))
  {
    noFixations_Begin <- 0
  }
  meanFixationDuration_Begin <- mean(fixations_Begin$dur)
  if (is.na(meanFixationDuration_Begin))
  {
    meanFixationDuration_Begin <- 0.0
  }
  fixationDurationTotal_Begin <- sum(fixations_Begin$dur)
  if (is.na(fixationDurationTotal_Begin))
  {
    fixationDurationTotal_Begin <- 0.0
  }
  percentFixations_Begin <- noFixations_Begin / noFixationsOverall

  fixations_Mid <- fixations[fixations[, "y"]>(minY + sizeY) & fixations[, "y"]<(maxY - sizeY),]
  noFixations_Mid <- nrow(fixations_Mid)
  if (is.na(noFixations_Mid))
  {
    noFixations_Mid <- 0
  }
  meanFixationDuration_Mid <- mean(fixations_Mid$dur)
  if (is.na(meanFixationDuration_Mid))
  {
    meanFixationDuration_Mid <- 0.0
  }
  fixationDurationTotal_Mid <- sum(fixations_Mid$dur)
  if (is.na(fixationDurationTotal_Mid))
  {
    fixationDurationTotal_Mid <- 0.0
  }

  fixations_End <- fixations[fixations[, "y"]>(maxY - sizeY) & fixations[, "y"]<=(maxY),]
  noFixations_End <- nrow(fixations_End)
  if (is.na(noFixations_End))
  {
    noFixations_End <- 0
  }
  meanFixationDuration_End <- mean(fixations_End$dur)
  if (is.na(meanFixationDuration_End))
  {
    meanFixationDuration_End <- 0.0
  }
  fixationDurationTotal_End <- sum(fixations_End$dur)
  if (is.na(fixationDurationTotal_End))
  {
    fixationDurationTotal_End <- 0.0
  }

 # print(nrow(features))

  vars <- list(user_id, text_id,
               noFixationsOverall, veryLongFixations, longFixations, shortFixations, meanFixationDuration, fixationRateOverall, totalDuration, medianFixationDuration,
               noFixations_Begin, noFixations_Mid, noFixations_End,
               meanFixationDuration_Begin, meanFixationDuration_Mid, meanFixationDuration_End,
               fixationDurationTotal_Begin, fixationDurationTotal_Mid, fixationDurationTotal_End,
               percentGazeSamples_Begin, percentGazeSamples_Mid, percentGazeSamples_End,
               noBacktrackSaccades, 0
  )
#  View(vars)
  #Calculate feature parameters
  features[nrow(features) + 1,] <- vars

  return(features)
}

###############################################################################
### CREATE FEATURE PARAMETER DATAFRAME & MERGE WITH PARTICIPANT RATINGS
###############################################################################

featureParameters <- process_raw_data(50, 6, 45)
text_features <- merge(featureParameters, participant_ratings, by=c("person.ID","text.ID"))

###############################################################################
### TRAIN RANDOM FOREST
###############################################################################

set.seed(10)
idx_Train <- sample(1:nrow(text_features), 2, replace = FALSE)
idx_Test <- !1:nrow(text_features) %in% idx_Train
featureColumns <- 3:(ncol(text_features)-8)

#COMPREHENSION MODEL
printf("*** COMPREHENSION MODEL ***\n")
comprehension.model <- randomForest(formula = comprehension ~ . - complexity - interest - familiarity - text.ID, data = text_features[idx_Train,], importance = TRUE,
                      ntree = 3000, mtry = 2)

comprehension.response <- predict(comprehension.model, text_features[idx_Test,])
comprehension.correct <- (abs(comprehension.response - text_features[idx_Test,]$comprehension) <= 1.0)

print(mean(comprehension.correct))
plot(comprehension.model)
varImpPlot(comprehension.model)

#COMPLEXITY MODEL
printf("*** COMPLEXITY MODEL ***\n")
complexity.model <- randomForest(formula = complexity ~ . - comprehension - interest - familiarity - text.ID, data = text_features[idx_Train,], importance = TRUE,
                      ntree = 3000, mtry = 2)

complexity.response <- predict(complexity.model, text_features[idx_Test,])
complexity.correct <- (abs(complexity.response - text_features[idx_Test,]$complexity) <= 1.0)

print(mean(complexity.correct))
plot(complexity.model)
varImpPlot(complexity.model)

#INTEREST MODEL
printf("*** INTEREST MODEL ***\n")
interest.model <- randomForest(formula = interest ~ . - complexity - comprehension - familiarity - text.ID, data = text_features[idx_Train,], importance = TRUE,
                      ntree = 3000, mtry = 4)

interest.response <- predict(interest.model, text_features[idx_Test,])
interest.correct <- (abs(interest.response - text_features[idx_Test,]$interest) <= 1.0)


print(mean(interest.correct))
plot(interest.model)
varImpPlot(interest.model)

#FAMILIARITY MODEL
printf("*** FAMILIARITY MODEL ***\n")
familiarity.model <- randomForest(formula = familiarity ~ . - complexity - comprehension - interest - text.ID, data = text_features[idx_Train,], importance = TRUE,
                      ntree = 3000, mtry = 2)

familiarity.response <- predict(familiarity.model, text_features[idx_Test,])
familiarity.correct <- (abs(familiarity.response - text_features[idx_Test,]$familiarity) <= 1.0)

print(mean(familiarity.correct))
plot(familiarity.model)
varImpPlot(familiarity.model)

###############################################################################
### DEBUG EXAMPLES FOR PLOT GRAPHING
###############################################################################

########Plot example gaze point graph
#plot(x=NULL, y=NULL, xlim=c(0, 1280), ylim=c(1024, 0)) #Create new screen graph, don't forget to set ylim to 1024,0 instead of 0,1024 otherwise you get a reversed image.
#points(p01_split$`2725`$L.POR.X..px., p01_split$`2725`$L.POR.Y..px.) #Plot points for text w/ id=`2725` on created graph.

#####Plot example filtered fixations graph
#testFilter2725 <- filtering(p01_split$`2725`, 50, 6, 45)
#plot(x=NULL, y=NULL, xlim=c(0, 1280), ylim=c(1024, 0))
#points(testFilter2725$x, testFilter2725$y)
#s <- seq(length(testFilter2725)-1)  # one shorter than data
#arrows(testFilter2725$x[s], testFilter2725$y[s], testFilter2725$x[s+1], testFilter2725$y[s+1], col= 1:3)
