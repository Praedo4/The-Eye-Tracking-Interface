require(emov)
require(xlsx)
require(rJava)
require(randomForest)
require(e1071)


full_participant_ratings <- data.frame()
full_participant_ratings <- read.xlsx("participant_ratings_2.xlsx", sheetName="user ratings")

feature_parameters <- data.frame()
#feature_parameters <- read.xlsx("feature_parameters.xlsx", sheetName="user ratings")
#feature_parameters <- feature_parameters[,0:6] #Drop not-used columns
#feature_parameters <- read.xlsx("measures.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_2.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_3.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_4.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_5.xlsx", sheetName="measures")
feature_parameters <- read.xlsx("measures_6.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_normalized.xlsx", sheetName="measures")
#feature_parameters <- feature_parameters[,0:26] #Drop not-used columns
#print(feature_parameters)

text_features <- data.frame()
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))
N <- nrow(text_features)


#set.seed(2)


#result <- rfcv(feature_parameters, factor(text_features$interest_label), cv.fold = 10)
#with(result, plot(n.var, error.cv, log="x", type="o", lwd=2))
#print(result$predicted)

require(EFS)

participant_ratings <- full_participant_ratings[,c(1,2,3)] #Drop not-used columns
text_features <- data.frame()
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))
N <- nrow(text_features)
fs_results <- ensemble_fs(text_features,39,selection=c(TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE,TRUE ))
print(fs_results)
efs_eval(data = text_features,efs_table = fs_results, file_name = 'feature_table', classnumber = 39, NA_threshold = 0.5,
         logreg = TRUE,
         rf = FALSE,
         permutation = TRUE, p_num = 2,
         variances = FALSE, jaccard = FALSE)
#barplot_fs("barplot",fs_results)

cor_threshold <- 0.3

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
participant_ratings <- full_participant_ratings[,c(5,1,2,3,4)]
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))
interesting_threshold <- 3.5
text_features$interest_label <- ifelse(text_features$interest > interesting_threshold,1,0)


library(ROSE)
test_function <- function(input_mask, number_of_tests = 100, mode = 'SVM'){
  result_mask <- 'x'  == names(text_features)
  print(input_mask)
  for (index in (1:length(input_mask))){
    result_mask <- result_mask | (names(text_features) == input_mask[index])
    #print(result_mask)
  }
  #print(result_mask)
  return (test_and_train(result_mask,number_of_tests, mode))
}

test_and_train <- function(feature_mask, number_of_tests = 100, mode = 'SVM', feature_selection_enabled = TRUE){
  feature_mask[39] <- FALSE
  feature_mask[40] <- TRUE
  feature_mask[41] <- TRUE
  #print(feature_mask)
  mean_accuracy <- 0
  total_percentage_of_ones <- 0
  total_percentage_of_zeroes <- 0
  total_correct_ones <- 0
  total_correct_zeroes <- 0
  number_of_tests_with_zeroes <- 0
  number_of_tests_with_ones <- 0
  #print(feature_mask)
  for(i in (1:number_of_tests)){
    repeat{
      #train_ids <- sample(1:nrow(text_features), N/10, replace = FALSE)
      #test_ids <- !1:nrow(text_features) %in% train_ids
      test_ids <- sample(1:nrow(text_features), N/10, replace = FALSE) 
      train_ids <- !1:nrow(text_features) %in% test_ids
      if(sum(text_features[train_ids,]$interest_label) < nrow(text_features[train_ids,])-2 && sum(text_features[train_ids,]$interest_label) > 2){
        break
      }
    }
    
    #print(sprintf("%d / %d",nrow(text_features[train_ids,]),nrow(text_features[test_ids,])))
    
    #print(mean(text_features[train_ids,feature_mask]$interest_label))
    
    training_data <- ROSE(interest_label ~ ., text_features[train_ids,feature_mask])
    
    #print(text_features[train_ids,feature_mask])
    if(mode == 'RF'){
    #interest.model <- randomForest(formula =  ~ . -comprehension - complexity - interest - interest_label  - familiarity - text.ID - person.ID, data = text_features[train_ids,], y = factor(text_features[train_ids,]$interest_label), importance = TRUE, 
    #                                    ntree = 500, mtry = 19)
      if(feature_selection_enabled){
       interest.model <- randomForest(formula =  ~ . -interest -interest_label, data = training_data$data, y = factor(training_data$data$interest_label), 
                                      ntree = 500, mtry = 1)
       #interest.model <- randomForest(formula =  ~ . -interest -interest_label, data = text_features[train_ids,feature_mask], y = factor(text_features[train_ids,]$interest_label), 
       #                              ntree = 500, mtry = 1)
      }
      else{
        interest.model <- randomForest(formula =  ~ .  -interest -interest_label, data = text_features[train_ids,], y = factor(text_features[train_ids,]$interest_label), 
                                      ntree = 500, mtry = 1)
      }
      interest.response <- predict(interest.model, text_features[test_ids,])
      responce_labels <- interest.response
      
    }
    else if(mode == 'RF_REG'){
      if(feature_selection_enabled){
        interest.model <- randomForest(formula = interest ~ .  - interest_label , data = text_features[train_ids,feature_mask], importance = TRUE, 
                                       ntree = 500, mtry = 1)
      }
      else{
        interest.model <- randomForest(formula = interest ~ .  - interest_label , data = text_features[train_ids,], importance = TRUE, 
                                       ntree = 500, mtry = 1)
      }
      interest.response <- predict(interest.model, text_features[test_ids,])
      responce_labels <- ifelse(interest.response > interesting_threshold , 1, 0)
      #responce_labels <- (abs(interest.response - text_features[test_ids,]$interest) <= 1.0)
      #interest.correct <- responce_labels
    }
    else if(mode == 'SVM'){
      if(feature_selection_enabled){
        interest.model <- svm(formula =  ~ . -interest -interest_label, data = training_data$data, y = factor(training_data$data$interest_label), type='C-classification', kernel =
                                "linear", degree = 3, gamma = 1,
                              coef0 = 0, cost = 1000, nu = 0.5,
                              class.weights = NULL, cachesize = 40, tolerance = 0.001, epsilon = 1,
                              shrinking = TRUE, cross = 0, probability = FALSE, fitted = TRUE)
        #interest.model <- svm(formula =  ~ . -interest -interest_label, data = text_features[train_ids,feature_mask], y = factor(text_features[train_ids,]$interest_label), type='C-classification', kernel =
        #                        "radial", degree = 3, gamma = 1,
        #                      coef0 = 0, cost = 1000, nu = 0.5,
        #                      class.weights = NULL, cachesize = 40, tolerance = 0.001, epsilon = 1,
        #                      shrinking = TRUE, cross = 0, probability = FALSE, fitted = TRUE)
      }
      else{
        interest.model <- svm(formula =  ~ . -interest -interest_label, data = text_features[train_ids,], y = factor(text_features[train_ids,]$interest_label), cost=100, gamma = 1)
      }
      interest.response <- predict(interest.model, text_features[test_ids,])
      responce_labels <- interest.response
      #responce_labels <- ifelse(interest.response > 3 , 1, 0)
    }
    else{
      print('INVALID MODE')
      return (0)
    }
    #interest.model <- svm(formula = interest ~ . -interest_label , data = text_features[train_ids,feature_mask], cost=1000, gamma = 0.01)
  
    #interest.model <- randomForest(formula = interest ~ . - interest - interest_label , data = text_features[train_ids,feature_mask], importance = TRUE, 
    #                                         ntree = 500, mtry = 2)
    
    interest.correct <- (responce_labels == text_features[test_ids,]$interest_label)
    
    ones <- (responce_labels == 1)
    zeroes <- (responce_labels == 0)
    number_of_samples <- length(ones)
    number_of_ones <- sum(ones)
    number_of_zeroes <- sum(zeroes)
    
    total_percentage_of_ones <- total_percentage_of_ones +  (number_of_ones / number_of_samples)
    total_percentage_of_zeroes <-  total_percentage_of_zeroes +  ( number_of_zeroes / number_of_samples)
    correct_ones <- interest.correct & ones
    correct_zeroes <- interest.correct & zeroes
    
    
    #if((sum(correct_ones)/number_of_ones)*(number_of_ones / number_of_samples) + (sum(correct_zeroes) / number_of_zeroes)*( number_of_zeroes / number_of_samples) != mean(interest.correct)){
    #  print("we have a problem")
    #  print(sprintf("%f + %f != %f",(sum(correct_ones)/number_of_ones)*(number_of_ones / number_of_samples),(sum(correct_zeroes) / number_of_zeroes)*( number_of_zeroes / number_of_samples),mean(interest.correct)))
    #}
    
    #if(is.nan(sum(correct_ones)/number_of_ones)){
    #  print(responce_labels)
    #}
    if(number_of_zeroes != 0){
       total_correct_zeroes <- total_correct_zeroes + (sum(correct_zeroes) / number_of_zeroes)
       number_of_tests_with_zeroes <- number_of_tests_with_zeroes + 1
    }
    if(number_of_ones != 0){
      total_correct_ones <- total_correct_ones + (sum(correct_ones)/number_of_ones)
      number_of_tests_with_ones <- number_of_tests_with_ones + 1
    }
    #print(mean(ones))
    
    #interest.correct <- (abs(interest.response - text_features[test_ids,]$interest) <= 1.0)
    
    mean_accuracy<- mean_accuracy + mean(interest.correct)
    #print(mean(interest.correct))
    #plot(interest.model)
    #varImpPlot(interest.model)
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
  print(sprintf("%% of 1s: %f %%",100*total_percentage_of_ones/number_of_tests))
  print(sprintf("%% of correct 1s: %f %%",100*correct_ones_portion))
  print(sprintf("%% of 0s: %f %%",100*total_percentage_of_zeroes/number_of_tests))
  print(sprintf("%% of correct 0s: %f %%",100*correct_zeroes_portion))
  print(sprintf("Mean accuracy: %f %%",100*(mean_accuracy/number_of_tests)))
  #return (correct_zeroes_portion)
  return (mean_accuracy/number_of_tests)
}

require(e1071)
require(FSelector)
#search_results <- best.first.search(names(text_features[,3:38]),test_function,max.backtracks = 5)
#search_results <- forward.search(names(text_features[,3:38]),test_function, max.backtracks = 20)
#print(mean(feature_parameters$interest_label))

search_results <- best.first.search(names(text_features[,3:38]),test_function)
#search_results <- forward.search(names(text_features[,fs_feature_mask]),test_function)


#saved_search_results <- search_results
test_function(search_results)


#interestRegression.model <- randomForest(formula = interest ~ . - comprehension - complexity - interest - interest_label  - familiarity - text.ID - person.ID, data = text_features[train_ids,], importance = TRUE, 
#                                    ntree = 500, mtry = 19)
#interestRegression.response <- predict(interestRegression.model, text_features[test_ids,])
#interestRegression.correct <- (abs(interestRegression.response - text_features[test_ids,]$interest) <= 1.0)
#
#print(mean(interestRegression.correct))
#plot(interestRegression.model)
#varImpPlot(interestRegression.model)
#
#comprehension.model <- randomForest(formula = comprehension ~ . - complexity - interest - familiarity - text.ID - person.ID, data = text_features[train_ids,], importance = TRUE, 
#                                    ntree = 500, mtry = 19)
#comprehension.response <- predict(comprehension.model, text_features[test_ids,])
#comprehension.correct <- (abs(comprehension.response - text_features[test_ids,]$comprehension) <= 1.0)
#
#print(mean(comprehension.correct))
#plot(comprehension.model)
#varImpPlot(comprehension.model)