install_packages <- function(){
  install.packages("installr")
  require(installr)
  updateR() #Update R
  
  install.packages("emov")
  install.packages("xlsx")
  install.packages("rJava")
  install.packages("randomForest")
  install.packages("e1071")
  install.packages("ROSE")
  install.packages("caret")
  install.packages("EFS")
  install.packages("FSelector")
}

require(emov)
require(xlsx)
require(rJava)
require(randomForest)
require(e1071)

library(ROSE)
library(caret)

require(EFS)
require(FSelector)

#input_file <- ''
input_file <- 'normalized'
interesting_threshold <- 3.5
cor_threshold <- 0.3

### START READING DATA ###
full_participant_ratings <- data.frame()
full_participant_ratings <- read.xlsx("participant_ratings_2.xlsx", sheetName="user ratings")

feature_parameters <- data.frame()

if(input_file == 'processed'){
  feature_parameters <- read.xlsx("measures.xlsx", sheetName="measures")
}
if(input_file == 'normalized'){
  feature_parameters <- read.xlsx("measures_normalized.xlsx", sheetName="measures")
}
#feature_parameters <- feature_parameters[,0:26] #Drop not-used columns
#print(feature_parameters)

text_features <- data.frame()
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))
N <- nrow(text_features)


#set.seed(2)


#result <- rfcv(feature_parameters, factor(text_features$interest_label), cv.fold = 10)
#with(result, plot(n.var, error.cv, log="x", type="o", lwd=2))
#print(result$predicted)


participant_ratings <- full_participant_ratings[,c(1,2,3,4)] #Drop not-used columns
text_features <- data.frame()
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))


text_features$interest_label <- ifelse(text_features$interest > interesting_threshold,1,0)
text_features <- text_features[,1:39]

N <- nrow(text_features)
### END READING DATA ###
### RUN FEATURE RANKING ###
fs_results <- ensemble_fs(text_features,39,selection=c(TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE ))
print(fs_results)
efs_eval(data = text_features,efs_table = fs_results, file_name = 'feature_table', classnumber = 39, NA_threshold = 0.5,
         logreg = TRUE,
         rf = FALSE,
         permutation = TRUE, p_num = 2,
         variances = FALSE, jaccard = FALSE)
#barplot_fs("barplot",fs_results)
### APPLY FEATURE FILTERING ##

framed_results <- data.frame(fs_results)
mean_scores <- rep(0,times = (ncol(framed_results)))
fs_feature_mask <- rep(FALSE,times = ncol(text_features))
for(i in 1:ncol(framed_results)){
  sum <- 0
  for(j in 1:nrow(framed_results)){
    if (!is.na(fs_results[j,i])){
      sum<- sum + fs_results[j,i]
    }
  }
  mean_scores[i] <- sum 
  if(sum > cor_threshold){
    fs_feature_mask[i] = TRUE
  }
}
fs_feature_mask[1] <- FALSE
fs_feature_mask[2] <- FALSE
#feature_mask[40] <- TRUE
#feature_mask[41] <- TRUE

feature_names <- names(framed_results)
output_mean_scores <- cbind(feature_names,mean_scores)
print(output_mean_scores[order(output_mean_scores[,2],decreasing = TRUE),])
#print(text_features[,feature_mask])

#full_text_features <- text_features
#text_features <- full_text_features
### RESTORE INPUT ARRAYS ###
participant_ratings <- full_participant_ratings[,c(5,6,1,2,3,4)]
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))
text_features$interest_label <- ifelse(text_features$interest > interesting_threshold,1,0)


###  DEFINING TEST FUNCTIONS FOR CLASSIFICATION MODEL ###
test_function_classification <- function(input_mask){
    
    result_mask <- 'x'  == names(text_features)
  print(input_mask)
  for (index in (1:length(input_mask))){
    result_mask <- result_mask | (names(text_features) == input_mask[index])
    #print(result_mask)
  }
  #print(result_mask)
  return (test_and_train_classification(result_mask,as.numeric(parameters[1]), parameters[2], parameters[3], parameters[4],parameters[5]))
}

test_and_train_classification <- function(feature_mask, number_of_tests = 100, mode = 'RF', scoring_function ='accuracy', balancing = 'NONE', output_file='null',feature_selection_enabled = TRUE){
  set.seed(4)
  feature_mask[39] <- FALSE #complexity
  feature_mask[40] <- FALSE #comprehension
  feature_mask[41] <- TRUE
  feature_mask[42] <- TRUE
  #print(feature_mask)
  mean_accuracy <- 0
  total_percentage_of_ones <- 0
  total_percentage_of_zeroes <- 0
  total_correct_ones <- 0
  total_correct_zeroes <- 0
  number_of_tests_with_zeroes <- 0
  number_of_tests_with_ones <- 0
  #print(feature_mask)
  for(i in (1:(number_of_tests/10))){
    repeat{
      folds <- createFolds(text_features[,1], k = 10)
      all_valid = TRUE;      
      for(j in (1:10)){
        test_ids <- folds[[j]]
        train_ids <- -folds[[j]]
        if(sum(text_features[train_ids,]$interest_label) >= nrow(text_features[train_ids,])-2 || sum(text_features[train_ids,]$interest_label) <= 2){
            all_valid = FALSE
        }
      }
      if(all_valid){
        break
      }
    }
    
    for(j in (1:10)){
      test_ids <- folds[[j]]
      train_ids <- -folds[[j]]
      if(balancing == 'ROSE'){
        training_data <- ROSE(interest_label ~ ., text_features[train_ids,feature_mask])$data
      }
      else if(feature_selection_enabled){
        training_data <- text_features[train_ids,feature_mask]
      }
      else{
        training_data <- text_features[train_ids,]
      }
      
      test_data <- text_features[test_ids,]
      
      #comprehension_training_features <- names(text_features) %in% c("Total.Reading.Time", "Mean.Pupil.Dilation", "Fixation.Dispersion.SD", "Fixation.Dispersion.Variance", "Saccade.Amplitude.Skewness", "Saccade.Mean.Velocity.Mean", "comprehension","interest","interest_label")  
      
      ###reg:
      #comprehension_training_features <- names(text_features) %in% c("Fixation.Number","Number.Of.Line.to.line.Saccades","Mean.Number.of.Regressions.Per.Line","Fixation.Duration.SD", "complexity",  "comprehension","interest","interest_label") 
      ###norm:
      ##comprehension_training_features <- names(text_features) %in% c("Total.Reading.Time","Mean.Pupil.Dilation","Fixation.Dispersion.SD","Fixation.Dispersion.Variance","Saccade.Amplitude.Skewness", "Saccade.Mean.Velocity.Mean", "complexity",  "comprehension","interest","interest_label") 
      #
      ##comprehension.model <- randomForest(formula = comprehension ~. -interest -interest_label, data = text_features[train_ids,comprehension_training_features], ntree = 500, mtry = 1) # -complexity
      #
      #comprehension.model <- svm(formula =  comprehension ~ . -interest -interest_label -complexity, data = text_features[train_ids,comprehension_training_features], cost = 1000, gamma=0.0001)
      #comprehension.response <- predict(comprehension.model, text_features[test_ids,comprehension_training_features])
      #test_data$comprehension <- comprehension.response
      
      ###reg
      #complexity_training_features <- names(text_features) %in% c("Scanpath.Length", "Regression.Rate", "complexity","comprehension","interest","interest_label")                         
      ###norm:
      ##complexity_training_features <- names(text_features) %in% c("Fixation.Rate","Mean.Number.of.Regressions.Per.Line","Fixation.Duration.Mean","Fixation.Duration.Skewness","Fixation.Duration.Kurtosis", "Saccade.Amplitude.Kurtosis", "complexity","comprehension","interest","interest_label")                         
#
      ##complexity.model <- randomForest(formula = complexity ~. -interest -interest_label , data = training_data, ntree = 500, mtry = 1)
      #complexity.model <- svm(formula =  complexity ~ . -interest -interest_label -comprehension, data = text_features[train_ids,complexity_training_features], cost = 1000, gamma=0.0001)
      #
      #complexity.response <- predict(complexity.model, text_features[test_ids,complexity_training_features])
      #test_data$complexity <- complexity.response
      
      if(mode == 'RF'){
         interest.model <- randomForest(formula =  ~ . -interest -interest_label, data = training_data, y = factor(training_data$interest_label), 
                                        ntree = 500, mtry = 1)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- interest.response
        
      }
      else if(mode == 'RF_REG'){
        interest.model <- randomForest(formula = interest ~ .  - interest_label , data = training_data, importance = TRUE, 
                                         ntree = 500, mtry = 1)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- ifelse(interest.response > interesting_threshold , 1, 0)
        #responce_labels <- (abs(interest.response - text_features[test_ids,]$interest) <= 1.0)
        #interest.correct <- responce_labels
      }
      else if(mode == 'SVM'){
          interest.model <- svm(formula =  ~ . -interest -interest_label, data = training_data, y = factor(training_data$interest_label), type='C-classification', kernel =
                                  "linear", degree = 3, gamma = 1,
                                coef0 = 0, cost = 1000, nu = 0.5,
                                class.weights = NULL, cachesize = 40, tolerance = 0.001, epsilon = 1,
                                shrinking = TRUE, cross = 0, probability = FALSE, fitted = TRUE)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- interest.response
        #responce_labels <- ifelse(interest.response > 3 , 1, 0)
      }
      
      else if(mode == 'SVM_RADIAL'){
        interest.model <- svm(formula =  ~ . -interest -interest_label, data = training_data, y = factor(training_data$interest_label), type='C-classification', kernel =
                                "radial", degree = 3, gamma = 1,
                              coef0 = 0, cost = 1000, nu = 0.5,
                              class.weights = NULL, cachesize = 40, tolerance = 0.001, epsilon = 1,
                              shrinking = TRUE, cross = 0, probability = FALSE, fitted = TRUE)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- interest.response
        #responce_labels <- ifelse(interest.response > 3 , 1, 0)
      }
      else{
        print('INVALID MODEL')
        return (0)
      }
      
      interest.correct <- (responce_labels == test_data$interest_label)
      
      ones <- (responce_labels == 1)
      zeroes <- (responce_labels == 0)
      number_of_samples <- length(ones)
      number_of_ones <- sum(ones)
      number_of_zeroes <- sum(zeroes)
      
      total_percentage_of_ones <- total_percentage_of_ones +  (number_of_ones / number_of_samples)
      total_percentage_of_zeroes <-  total_percentage_of_zeroes +  ( number_of_zeroes / number_of_samples)
      correct_ones <- interest.correct & ones
      correct_zeroes <- interest.correct & zeroes
      
      
      if(number_of_zeroes != 0){
         total_correct_zeroes <- total_correct_zeroes + (sum(correct_zeroes) / number_of_zeroes)
         number_of_tests_with_zeroes <- number_of_tests_with_zeroes + 1
      }
      if(number_of_ones != 0){
        total_correct_ones <- total_correct_ones + (sum(correct_ones)/number_of_ones)
        number_of_tests_with_ones <- number_of_tests_with_ones + 1
      }
      
      mean_accuracy<- mean_accuracy + mean(interest.correct)
      }
  }
  
  if(number_of_tests_with_zeroes==0){
    correct_zeroes_portion <- 0
  }
  else{
    correct_zeroes_portion <- total_correct_zeroes/number_of_tests
  }
  if(number_of_tests_with_ones == 0){
    correct_ones_portion <- 0
  }
  else{
    correct_ones_portion <- total_correct_ones/number_of_tests
  }
  #plot(interest.model)
  #varImpPlot(interest.model)
  if(output_file != 'null'){
    print(sprintf("Printing to : %s", output_file))
    sink(file=output_file)
    print(sprintf("Model: %s\tBALANCING: %s\t Scoring function: %s",mode,balancing,scoring_function))
    print(sprintf("%% of 1s: %.2f %%",100*total_percentage_of_ones/number_of_tests))
    print(sprintf("%% of correct 1s: %.2f %%",100*correct_ones_portion))
    print(sprintf("%% of 0s: %.2f %%",100*total_percentage_of_zeroes/number_of_tests))
    print(sprintf("%% of correct 0s: %.2f %%",100*correct_zeroes_portion))
    print(sprintf("Mean accuracy: %.2f %%",100*(mean_accuracy/number_of_tests)))
    print(names(text_features)[feature_mask])
    sink(file=NULL)
  }
  if(scoring_function == 'accuracy'){
    return (mean_accuracy/number_of_tests)
  }
  else{
    return (correct_zeroes_portion)
  }
}

###  DEFINING TEST FUNCTIONS FOR REGRESSION MODELS ###

test_function_regression <- function(input_mask){
  result_mask <- 'x'  == names(text_features)
  print(input_mask)
  for (index in (1:length(input_mask))){
    result_mask <- result_mask | (names(text_features) == input_mask[index])
    #print(result_mask)
  }
  #print(result_mask)
  return (test_and_train_regression(result_mask,as.numeric(parameters[1]), parameters[2], as.numeric(parameters[3]), parameters[4],parameters[5]))
}


test_and_train_regression <- function(feature_mask, number_of_tests = 100, mode = 'RF', dist = 1, target = 'interest', output_file='null',feature_selection_enabled = TRUE){
  set.seed(4)
  feature_mask[39] <- target=='complexity' #complexity
  feature_mask[40] <- target=='comprehension' #comprehension
  feature_mask[41] <- FALSE
  feature_mask[42] <- target=='interest'
  #print(feature_mask)
  #print(feature_mask)
  mean_accuracy <- 0
  mse <- 0
  for(i in (1:(number_of_tests/10))){
    folds <- createFolds(text_features[,1], k = 10)
    
    for(j in (1:10)){
      test_ids <- folds[[j]]
      train_ids <- -folds[[j]]
      if(feature_selection_enabled){
        training_data <- text_features[train_ids,feature_mask]
      }
      else{
        training_data <- text_features[train_ids,]
      }
      
      if(mode == 'RF'){
        if(target == 'interest'){
        regression.model <- randomForest(formula =   interest ~ . , data = training_data, 
                                       ntree = 500, mtry = 1)
        }
        if(target == 'complexity'){
          regression.model <- randomForest(formula =   complexity ~ . , data = training_data, 
                                         ntree = 500, mtry = 1)
        }
        if(target == 'comprehension'){
          regression.model <- randomForest(formula =   comprehension ~ . , data = training_data, 
                                           ntree = 500, mtry = 1)
        }
        regression.response <- predict(regression.model, text_features[test_ids,])
        responce_labels <- regression.response
        
      }
      else if(mode == 'RF_REG'){
        if(target == 'interest'){
          regression.model <- randomForest(formula =   interest ~ . , data = training_data, 
                                           ntree = 500, mtry = 1)
        }
        if(target == 'complexity'){
          regression.model <- randomForest(formula =   complexity ~ . , data = training_data, 
                                           ntree = 500, mtry = 1)
        }
        if(target == 'comprehension'){
          regression.model <- randomForest(formula =   comprehension ~ . , data = training_data, 
                                           ntree = 500, mtry = 1)
        }
        
        regression.response <- predict(regression.model, text_features[test_ids,])
        responce_labels <- ifelse(regression.response > interesting_threshold , 1, 0)
        #responce_labels <- (abs(interest.response - text_features[test_ids,]$interest) <= 1.0)
        #interest.correct <- responce_labels
      } 
      else if(mode == 'SVM' || mode == 'SVM_RADIAL'){
        if(target == 'interest'){
          regression.model <- svm(formula =  interest ~ . , data = training_data, cost = 1000, gamma=0.0001)
        }
        if(target == 'complexity'){
          regression.model <- svm(formula =  complexity ~ . , data = training_data, cost = 1000, gamma=0.0001)
        }
        if(target == 'comprehension'){
          regression.model <- svm(formula =  comprehension ~ . , data = training_data, cost = 1000, gamma=0.0001)
        }
        
        regression.response <- predict(regression.model, text_features[test_ids,])
        responce_labels <- regression.response
        #responce_labels <- ifelse(interest.response > 3 , 1, 0)
      }
      else{
        print('INVALID MODEL')
        return (0)
      }
      
      if(target == 'interest'){
        regression.correct <- abs(responce_labels - text_features[test_ids,]$interest) < dist
        mse <- mse + mean((responce_labels - text_features[test_ids,]$interest)*(responce_labels - text_features[test_ids,]$interest))
      }
      if(target == 'complexity'){
        regression.correct <- abs(responce_labels - text_features[test_ids,]$complexity) < dist
        mse <- mse + mean((responce_labels - text_features[test_ids,]$complexity)*(responce_labels - text_features[test_ids,]$complexity))
      }
      if(target == 'comprehension'){
        regression.correct <- abs(responce_labels - text_features[test_ids,]$comprehension) < dist
        mse <- mse + mean((responce_labels - text_features[test_ids,]$comprehension)*(responce_labels - text_features[test_ids,]$comprehension))
      }
      
      
      mean_accuracy<- mean_accuracy + mean(regression.correct)
    }
  }
  
  #plot(interest.model)
  #varImpPlot(interest.model)
  if(output_file != 'null'){
    sink(file=output_file)
    print(sprintf("Model: %s Dist: %s",mode,dist))
    print(sprintf("Mean accuracy: %.2f %%",100*(mean_accuracy/number_of_tests)))
    print(sprintf("MSE: %.2f ",(mse/number_of_tests)))
    print(names(text_features)[feature_mask])
    sink(file=NULL)
  }
  print(mean_accuracy/number_of_tests)
  return (mean_accuracy/number_of_tests)
}


###  DEFINING TEST FUNCTIONS FOR CLASSIFICATION MODEL USING PREDICTION ###
test_function_classification_predicted <- function(input_mask){
  
  result_mask <- 'x'  == names(text_features)
  print(input_mask)
  for (index in (1:length(input_mask))){
    result_mask <- result_mask | (names(text_features) == input_mask[index])
    #print(result_mask)
  }
  #print(result_mask)
  return (test_and_train_classification_predicted(result_mask,as.numeric(parameters[1]), parameters[2], parameters[3], parameters[4],parameters[5]))
}

test_and_train_classification_predicted <- function(feature_mask, number_of_tests = 100, mode = 'RF', complexity_flag ='0', comprehension_flag = '0', output_file='null',feature_selection_enabled = TRUE){
  set.seed(4)
  feature_mask[39] <- complexity_flag!='0' #complexity
  feature_mask[40] <- comprehension_flag!='0' #comprehension
  feature_mask[41] <- TRUE
  feature_mask[42] <- TRUE
  #print(feature_mask)
  mean_accuracy <- 0
  total_percentage_of_ones <- 0
  total_percentage_of_zeroes <- 0
  total_correct_ones <- 0
  total_correct_zeroes <- 0
  number_of_tests_with_zeroes <- 0
  number_of_tests_with_ones <- 0
  #print(feature_mask)
  for(i in (1:(number_of_tests/10))){
    repeat{
      folds <- createFolds(text_features[,1], k = 10)
      all_valid = TRUE;      
      for(j in (1:10)){
        test_ids <- folds[[j]]
        train_ids <- -folds[[j]]
        if(sum(text_features[train_ids,]$interest_label) >= nrow(text_features[train_ids,])-2 || sum(text_features[train_ids,]$interest_label) <= 2){
          all_valid = FALSE
        }
      }
      if(all_valid){
        break
      }
    }
    
    for(j in (1:10)){
      test_ids <- folds[[j]]
      train_ids <- -folds[[j]]
      if(feature_selection_enabled){
        training_data <- text_features[train_ids,feature_mask]
      }
      else{
        training_data <- text_features[train_ids,]
      }
      
      test_data <- text_features[test_ids,]
      
      if(comprehension_flag == '2'){ # predicting comprehension
        
          if(input_file=='normalized'){
            ##norm:
            comprehension_training_features <- names(text_features) %in% c("Total.Reading.Time","Mean.Pupil.Dilation","Fixation.Dispersion.SD","Fixation.Dispersion.Variance","Saccade.Amplitude.Skewness", "Saccade.Mean.Velocity.Mean", "complexity",  "comprehension","interest","interest_label") 
          }
          else{
            ##reg:
            comprehension_training_features <- names(text_features) %in% c("Fixation.Number","Number.Of.Line.to.line.Saccades","Mean.Number.of.Regressions.Per.Line","Fixation.Duration.SD", "complexity",  "comprehension","interest","interest_label") 
          }
        
        if(complexity_flag == '0'){
          comprehension.model <- svm(formula =  comprehension ~ . -interest -interest_label, data = text_features[train_ids,comprehension_training_features], cost = 1000, gamma=0.0001)
        }
        else{
          comprehension.model <- svm(formula =  comprehension ~ . -interest -interest_label -complexity, data = text_features[train_ids,comprehension_training_features], cost = 1000, gamma=0.0001)
        }
        comprehension.response <- predict(comprehension.model, text_features[test_ids,comprehension_training_features])
        test_data$comprehension <- comprehension.response
      }
      
      
      if(complexity_flag == '2'){ #predicting complexity
        
          if(input_file=='normalized'){
            ##norm:
            complexity_training_features <- names(text_features) %in% c("Fixation.Rate","Mean.Number.of.Regressions.Per.Line","Fixation.Duration.Mean","Fixation.Duration.Skewness","Fixation.Duration.Kurtosis", "Saccade.Amplitude.Kurtosis", "complexity","comprehension","interest","interest_label")                         
          }
          else{
            ##reg:
            complexity_training_features <- names(text_features) %in% c("Scanpath.Length", "Regression.Rate", "complexity","comprehension","interest","interest_label")                         
          }
        
        if(comprehension_flag == '0'){
          complexity.model <- svm(formula =  complexity ~ . -interest -interest_label, data = text_features[train_ids,complexity_training_features], cost = 1000, gamma=0.0001)
        }
        else{
          complexity.model <- svm(formula =  complexity ~ . -interest -interest_label -comprehension, data = text_features[train_ids,complexity_training_features], cost = 1000, gamma=0.0001)
        }
        complexity.response <- predict(complexity.model, text_features[test_ids,complexity_training_features])
        test_data$complexity <- complexity.response
      }
      
      
      if(mode == 'RF'){
        interest.model <- randomForest(formula =  ~ . -interest -interest_label, data = training_data, y = factor(training_data$interest_label), 
                                       ntree = 500, mtry = 1)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- interest.response
        
      }
      else if(mode == 'RF_REG'){
        interest.model <- randomForest(formula = interest ~ .  - interest_label , data = training_data, importance = TRUE, 
                                       ntree = 500, mtry = 1)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- ifelse(interest.response > interesting_threshold , 1, 0)
        #responce_labels <- (abs(interest.response - text_features[test_ids,]$interest) <= 1.0)
        #interest.correct <- responce_labels
      }
      else if(mode == 'SVM'){
        interest.model <- svm(formula =  ~ . -interest -interest_label, data = training_data, y = factor(training_data$interest_label), type='C-classification', kernel =
                                "linear", degree = 3, gamma = 1,
                              coef0 = 0, cost = 1000, nu = 0.5,
                              class.weights = NULL, cachesize = 40, tolerance = 0.001, epsilon = 1,
                              shrinking = TRUE, cross = 0, probability = FALSE, fitted = TRUE)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- interest.response
        #responce_labels <- ifelse(interest.response > 3 , 1, 0)
      }
      
      else if(mode == 'SVM_RADIAL'){
        interest.model <- svm(formula =  ~ . -interest -interest_label, data = training_data, y = factor(training_data$interest_label), type='C-classification', kernel =
                                "radial", degree = 3, gamma = 1,
                              coef0 = 0, cost = 1000, nu = 0.5,
                              class.weights = NULL, cachesize = 40, tolerance = 0.001, epsilon = 1,
                              shrinking = TRUE, cross = 0, probability = FALSE, fitted = TRUE)
        
        interest.response <- predict(interest.model, test_data)
        responce_labels <- interest.response
        #responce_labels <- ifelse(interest.response > 3 , 1, 0)
      }
      else{
        print('INVALID MODEL')
        return (0)
      }
      
      interest.correct <- (responce_labels == test_data$interest_label)
      
      ones <- (responce_labels == 1)
      zeroes <- (responce_labels == 0)
      number_of_samples <- length(ones)
      number_of_ones <- sum(ones)
      number_of_zeroes <- sum(zeroes)
      
      total_percentage_of_ones <- total_percentage_of_ones +  (number_of_ones / number_of_samples)
      total_percentage_of_zeroes <-  total_percentage_of_zeroes +  ( number_of_zeroes / number_of_samples)
      correct_ones <- interest.correct & ones
      correct_zeroes <- interest.correct & zeroes
      
      
      if(number_of_zeroes != 0){
        total_correct_zeroes <- total_correct_zeroes + (sum(correct_zeroes) / number_of_zeroes)
        number_of_tests_with_zeroes <- number_of_tests_with_zeroes + 1
      }
      if(number_of_ones != 0){
        total_correct_ones <- total_correct_ones + (sum(correct_ones)/number_of_ones)
        number_of_tests_with_ones <- number_of_tests_with_ones + 1
      }
      
      mean_accuracy<- mean_accuracy + mean(interest.correct)
    }
  }
  
  if(number_of_tests_with_zeroes==0){
    correct_zeroes_portion <- 0
  }
  else{
    correct_zeroes_portion <- total_correct_zeroes/number_of_tests
  }
  if(number_of_tests_with_ones == 0){
    correct_ones_portion <- 0
  }
  else{
    correct_ones_portion <- total_correct_ones/number_of_tests
  }
  #plot(interest.model)
  #varImpPlot(interest.model)
  if(output_file != 'null'){
    print(sprintf("Printing to : %s", output_file))
    sink(file=output_file)
    print(sprintf("Model: %s\tComplexity: %s\t Comprehension: %s",mode,complexity_flag,comprehension_flag))
    print(sprintf("%% of 1s: %.2f %%",100*total_percentage_of_ones/number_of_tests))
    print(sprintf("%% of correct 1s: %.2f %%",100*correct_ones_portion))
    print(sprintf("%% of 0s: %.2f %%",100*total_percentage_of_zeroes/number_of_tests))
    print(sprintf("%% of correct 0s: %.2f %%",100*correct_zeroes_portion))
    print(sprintf("Mean accuracy: %.2f %%",100*(mean_accuracy/number_of_tests)))
    print(names(text_features)[feature_mask])
    sink(file=NULL)
  }
  return (mean_accuracy/number_of_tests)
}


### RUNNING THE TEST(s) ###

#### APLLYING ROSE TO THE FULL DATASET ###
#original_data <- text_features 
#text_features <- ROSE(interest_label ~ ., text_features)$data
#### END ROSE ###

#### TO RECOVER THE DATA BEFORE ROSE
#text_features <- original_data 
#### END RECOVERY ####

models_list <- c("RF","SVM_RADIAL")
scoring_function_list <- c("accuracy","zeroes")
search_list <- c("best-first","forward")
balancing_list <-c("NONE","ROSE")

### START TESTING FOR CLASIFICATION ####
###run_classification_test <- function(){
  
  TEST_MODELS <- TRUE
  TEST_SCORING_FUNCTION <- TRUE
  TEST_SEARCH <- FALSE
  TEST_BALANCING <- TRUE
  number_of_tests_to_run <- 2 * ifelse(TEST_MODELS, length(models_list),1) * ifelse(TEST_SCORING_FUNCTION, length(scoring_function_list),1) * ifelse(TEST_SEARCH, length(search_list),1) * ifelse(TEST_BALANCING, length(balancing_list),1)
  for(test_index in (0:number_of_tests_to_run)){
    parameters <- c('100','RF','zeroes','NONE','null') #initialize parameters
    updated_test_index <- floor(test_index /2)
    if(TEST_MODELS){
      cur_model <- models_list[updated_test_index %% length(models_list) + 1]
      updated_test_index <- floor(updated_test_index /length(models_list))
    }
    else{
      cur_model <- models_list[1]
    }
    
    if(TEST_SCORING_FUNCTION){
      cur_scoring_function <- scoring_function_list[updated_test_index %% length(scoring_function_list) + 1]
      updated_test_index <- floor(updated_test_index /length(scoring_function_list))
    }
    else{
      cur_scoring_function <- scoring_function_list[1]
    }
    
    if(TEST_SEARCH){
      cur_search <- search_list[updated_test_index %% length(search_list) + 1]
      updated_test_index <- floor(updated_test_index /length(search_list))
    }
    else{
      cur_search <- search_list[1]
    }
    
    
    if(TEST_BALANCING){
      cur_balancing <- balancing_list[updated_test_index %% length(balancing_list) + 1]
      updated_test_index <- floor(updated_test_index /length(balancing_list))
    }
    else{
      cur_balancing <- balancing_list[1]
    }
    
    parameters <- c('100', cur_model, cur_scoring_function, cur_balancing,'null')
    if((test_index%% 2) == 0){
      
      if(cur_search == 'best-first'){
        search_results <- best.first.search(names(text_features[,fs_feature_mask]),test_function_classification)
      }
      else{
        search_results <- forward.search(names(text_features[,fs_feature_mask]),test_function_classification)
      }
    }
    else{
      if(cur_search == 'best-first'){
        search_results <- best.first.search(names(text_features[,3:38]),test_function_classification)
      }
      else{
        search_results <- forward.search(names(text_features[,3:38]),test_function_classification)
      }
      
    }
    
    if((test_index %% 2) == 0){
      parameters <- c('100',cur_model, cur_scoring_function, cur_balancing, sprintf("%d%s_results%s_0.3/2%s_%s_%s_%s.txt",floor(interesting_threshold),ifelse(interesting_threshold-floor(interesting_threshold)==0,'','.5'),ifelse(input_file=='normalized','_normalized',''),cur_model,cur_scoring_function,cur_search,cur_balancing))
    }
    else{
      parameters <- c('100',cur_model, cur_scoring_function, cur_balancing, sprintf("%d%s_results%s_0/2%s_%s_%s_%s.txt",floor(interesting_threshold),ifelse(interesting_threshold-floor(interesting_threshold)==0,'','.5'),ifelse(input_file=='normalized','_normalized',''),cur_model,cur_scoring_function,cur_search,cur_balancing))
   }
    
    #### SAVE THE RESULTS
    #saved_search_results <- search_results
    
    test_function_classification(search_results)
  }
  
  
#}
  
### END TESTING FOR CLASIFICATION ####
  
### START TESTING FOR REGRESSION ####
###run_regression_test <- function(){
  dist_list <- c('1','1.5','2')
  
  TEST_MODELS <- TRUE
  TEST_DIST <- TRUE
  TEST_SEARCH <- FALSE
  regression_target <- 'interest' 
  #regression_target <- 'complexity'
  #regression_target <- 'comprehension'
  number_of_tests_to_run <- ifelse(TEST_MODELS, length(models_list),1) * ifelse(TEST_DIST, length(dist_list),1) * ifelse(TEST_SEARCH, length(search_list),1) 
  
  for(test_index in (0:number_of_tests_to_run)){
    
    updated_test_index <- test_index
    if(TEST_MODELS){
      cur_model <- models_list[updated_test_index %% length(models_list) + 1]
      updated_test_index <- floor(updated_test_index /length(models_list))
    }
    else{
      cur_model <- models_list[1]
    }
    
    if(TEST_DIST){
      cur_dist <- dist_list[updated_test_index %% length(dist_list) + 1]
      updated_test_index <- floor(updated_test_index /length(dist_list))
    }
    else{
      cur_dist <- dist_list[1]
    }
    
    if(TEST_SEARCH){
      cur_search <- search_list[updated_test_index %% length(search_list) + 1]
      updated_test_index <- floor(updated_test_index /length(search_list))
    }
    else{
      cur_search <- search_list[1]
    }
    
    
    parameters <- c('100', cur_model, cur_dist, regression_target,'null')
    if(cur_search == 'best-first'){
        search_results <- best.first.search(names(text_features[,3:38]),test_function_regression)
      }
    else{
        search_results <- forward.search(names(text_features[,3:38]),test_function_regression)
      }
    
    
    parameters <- c('100',cur_model, cur_dist, regression_target, sprintf("results%s_for_%s/%s_%s.txt",ifelse(input_file=='normalized','_normalized',''),regression_target,cur_model,cur_dist))
    
    #print(sprintf("%s_%s_%s_%s.txt",cur_model,cur_scoring_function,cur_search,cur_balancing))
    
    #saved_search_results <- search_results
    #test_function(search_results,100,'SVM','accuracy','NONE','svm_linear_full_rose_backward.txt')
    #test_function(search_results,100,'RF','accuracy','NONE', 'norm_rf_a_0_original_forward.txt')
    
    test_function_regression(search_results)
  }
###}
### END TESTING FOR REGRESSION ####
  
  
  
### START TESTING FOR CLASSIFICATION WITH COMPLEXITY/COMPREHENSION ####
###run_classification_with_complexity_comprehension <- function(){
  #complexity_list <- c('0','1','2') # 0 - None, 1 - Original, 2 - predicted
  complexity_list <- c('2','0') # 0 - None, 1 - Original, 2 - predicted
  comprehension_list <- c('2','0') # 0 - None, 1 - Original, 2 - predicted
  #comprehension_list <- c('0','1','2') # 0 - None, 1 - Original, 2 - predicted
  TEST_MODELS <- TRUE
  TEST_COMPLEXITY <- TRUE
  TEST_COMPREHENSION <- TRUE
  number_of_tests_to_run <- 2* ifelse(TEST_MODELS, length(models_list),1) * ifelse(TEST_COMPLEXITY, length(comprehension_list),1) * ifelse(TEST_COMPREHENSION, length(comprehension_list),1) 
  for(test_index in (0:number_of_tests_to_run)){
    parameters <- c('100','RF','zeroes','NONE','null') #initialize parameters
    updated_test_index <- floor(test_index /2)
    if(TEST_MODELS){
      cur_model <- models_list[updated_test_index %% length(models_list) + 1]
      updated_test_index <- floor(updated_test_index /length(models_list))
    }
    else{
      cur_model <- models_list[1]
    }
    
    if(TEST_COMPLEXITY){
      cur_complexity <- complexity_list[updated_test_index %% length(complexity_list) + 1]
      updated_test_index <- floor(updated_test_index /length(complexity_list))
    }
    else{
      cur_complexity <- complexity_list[1]
    }
    
    if(TEST_COMPREHENSION){
      cur_comprehension <- comprehension_list[updated_test_index %% length(comprehension_list) + 1]
      updated_test_index <- floor(updated_test_index /length(comprehension_list))
    }
    else{
      cur_comprehension <- comprehension_list[1]
    }
    
    
    
    parameters <- c('100', cur_model, cur_complexity, cur_comprehension,'null')
    if((test_index%% 2) == 0){
        search_results <- best.first.search(names(text_features[,fs_feature_mask]),test_function_classification_predicted)
    }
    else{
        search_results <- best.first.search(names(text_features[,3:38]),test_function_classification_predicted)
     }
    
    if((test_index %% 2) == 0){
      parameters <- c('100',cur_model, cur_complexity, cur_comprehension, sprintf("%d%s_results%s%s%s%s_0.3/2%s.txt",floor(interesting_threshold),ifelse(interesting_threshold-floor(interesting_threshold)==0,'','.5'),ifelse(input_file=='normalized','_normalized',''),ifelse(cur_complexity!='0','_complexity',''),ifelse(cur_comprehension!='0','_comprehension',''),ifelse(cur_complexity=='2'||cur_comprehension=='2','(predicted)',''),cur_model))
    }
    else{
      parameters <- c('100',cur_model, cur_scoring_function, cur_balancing, sprintf("%d%s_results%s%s%s%s_0/2%s.txt",floor(interesting_threshold),ifelse(interesting_threshold-floor(interesting_threshold)==0,'','.5'),ifelse(input_file=='normalized','_normalized',''),ifelse(cur_complexity!='0','_complexity',''),ifelse(cur_comprehension!='0','_comprehension',''),ifelse(cur_complexity=='2'||cur_comprehension=='2','(predicted)',''),cur_model))
    }
    
    #### SAVE THE RESULTS
    #saved_search_results <- search_results
    
    test_function_classification_predicted(search_results)
  }
###}

