require(emov)
require(xlsx)
require(rJava)
require(randomForest)


participant_ratings <- data.frame()
participant_ratings <- read.xlsx("participant_ratings_2.xlsx", sheetName="user ratings")
participant_ratings <- participant_ratings[,0:7] #Drop not-used columns

feature_parameters <- data.frame()
#feature_parameters <- read.xlsx("feature_parameters.xlsx", sheetName="user ratings")
#feature_parameters <- feature_parameters[,0:6] #Drop not-used columns
#feature_parameters <- read.xlsx("measures.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_2.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_3.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_4.xlsx", sheetName="measures")
#feature_parameters <- read.xlsx("measures_5.xlsx", sheetName="measures")
feature_parameters <- read.xlsx("measures_6.xlsx", sheetName="measures")
#feature_parameters <- feature_parameters[,0:26] #Drop not-used columns
#print(feature_parameters)

text_features <- data.frame()
text_features <- merge(feature_parameters, participant_ratings, by=c("person.ID","text.ID"))
N <- nrow(text_features)
#trainN <- floor((9*N)/10)
#train_ids <- (1 : trainN)
#test_ids <- (trainN + 1 : (N - trainN))

#set.seed(2)

#feature_ids <- (3:6)

#print(text_features[3:6])
#result <- rfcv(feature_parameters, factor(text_features$interest_label), cv.fold = 10)
#with(result, plot(n.var, error.cv, log="x", type="o", lwd=2))
#print(result$predicted)


train_ids <- sample(1:nrow(text_features), N/10, replace = FALSE)
test_ids <- !1:nrow(text_features) %in% train_ids

interest.model <- randomForest(formula =  ~ . -comprehension - complexity - interest - interest_label  - familiarity - text.ID - person.ID, data = text_features[train_ids,], y = factor(text_features[train_ids,]$interest_label), importance = TRUE, 
                                    ntree = 500, mtry = 19)
interest.response <- predict(interest.model, text_features[test_ids,])
interest.correct <- (interest.response == text_features[test_ids,]$interest_label)
#interest.correct <- (abs(interest.response - text_features[test_ids,]$interest) <= 1.0)

print(mean(interest.correct))
plot(interest.model)
varImpPlot(interest.model)


interestRegression.model <- randomForest(formula = interest ~ . - comprehension - complexity - interest - interest_label  - familiarity - text.ID - person.ID, data = text_features[train_ids,], importance = TRUE, 
                                    ntree = 500, mtry = 19)
interestRegression.response <- predict(interestRegression.model, text_features[test_ids,])
interestRegression.correct <- (abs(interestRegression.response - text_features[test_ids,]$interest) <= 1.0)

print(mean(interestRegression.correct))
plot(interestRegression.model)
varImpPlot(interestRegression.model)

comprehension.model <- randomForest(formula = comprehension ~ . - complexity - interest - familiarity - text.ID - person.ID, data = text_features[train_ids,], importance = TRUE, 
                                    ntree = 500, mtry = 19)
comprehension.response <- predict(comprehension.model, text_features[test_ids,])
comprehension.correct <- (abs(comprehension.response - text_features[test_ids,]$comprehension) <= 1.0)

print(mean(comprehension.correct))
plot(comprehension.model)
varImpPlot(comprehension.model)