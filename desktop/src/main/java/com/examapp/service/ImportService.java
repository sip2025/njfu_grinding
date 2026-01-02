package com.examapp.service;

import com.examapp.data.QuestionManager;
import com.examapp.model.Question;
import com.examapp.model.Subject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 题库导入服务
 */
public class ImportService {
    private final Gson gson;
    private final QuestionManager questionManager;

    public ImportService() {
        this.gson = new Gson();
        this.questionManager = QuestionManager.getInstance();
    }

    /**
     * 从JSON文件导入题库
     */
    public Subject importFromJson(File file) throws IOException {
        System.out.println("[ImportService] ========== 开始导入题库 ==========");
        System.out.println("[ImportService] 文件路径: " + file.getAbsolutePath());
        System.out.println("[ImportService] 文件大小: " + file.length() + " 字节");
        
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            System.out.println("[ImportService] 开始解析JSON...");
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            if (jsonObject == null) {
                System.err.println("[ImportService] 错误：JSON对象为null");
                throw new IOException("无法解析JSON文件");
            }
            
            System.out.println("[ImportService] JSON根对象keys: " + jsonObject.keySet());
            
            // 创建题库对象
            Subject subject = new Subject();
            subject.setId(UUID.randomUUID().toString());
            
            // 获取题库名称
            String subjectName = file.getName().replace(".json", "");
            if (jsonObject.has("name")) {
                subjectName = jsonObject.get("name").getAsString();
                System.out.println("[ImportService] 从JSON读取题库名: " + subjectName);
            } else {
                System.out.println("[ImportService] 使用文件名作为题库名: " + subjectName);
            }
            subject.setName(subjectName);
            subject.setDisplayName(subjectName);
            
            // 解析题目列表
            List<Question> questions = new ArrayList<>();
            
            // 检查是标准格式还是Android格式
            boolean hasQuestionsField = jsonObject.has("questions");
            boolean hasAndroidFormat = jsonObject.has("单选题") || jsonObject.has("多选题") || jsonObject.has("判断题");
            
            if (hasQuestionsField) {
                // 标准格式
                System.out.println("[ImportService] 使用标准格式解析");
                JsonArray questionsArray = jsonObject.getAsJsonArray("questions");
                
                if (questionsArray == null) {
                    System.err.println("[ImportService] 错误：'questions'不是数组");
                    throw new IOException("JSON格式错误：'questions'必须是数组");
                }
                
                questions = parseStandardFormat(questionsArray);
                
            } else if (hasAndroidFormat) {
                // Android格式
                System.out.println("[ImportService] 使用Android格式解析");
                questions = parseAndroidFormat(jsonObject);
                
            } else {
                System.err.println("[ImportService] 错误：未知的JSON格式");
                throw new IOException("JSON格式错误：需要 'questions' 字段或 '单选题/多选题/判断题' 字段");
            }
            
            System.out.println("[ImportService] 解析完成，总题数: " + questions.size());
            
            subject.setQuestions(questions);
            subject.setSortOrder(questionManager.getAllSubjects().size());
            
            // 保存到题库管理器
            System.out.println("[ImportService] 保存到题库管理器...");
            questionManager.addSubject(subject);
            System.out.println("[ImportService] ========== 导入完成 ==========");
            
            return subject;
        } catch (Exception e) {
            System.err.println("[ImportService] 导入失败: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new IOException("导入失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析标准格式题库
     */
    private List<Question> parseStandardFormat(JsonArray questionsArray) {
        List<Question> questions = new ArrayList<>();
        System.out.println("[ImportService] 题目总数: " + questionsArray.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < questionsArray.size(); i++) {
            JsonElement element = questionsArray.get(i);
            if (!element.isJsonObject()) {
                System.err.println("[ImportService] 警告：第" + (i+1) + "个元素不是对象，跳过");
                failCount++;
                continue;
            }
            
            JsonObject questionObj = element.getAsJsonObject();
            Question question = parseQuestion(questionObj);
            if (question != null) {
                questions.add(question);
                successCount++;
            } else {
                System.err.println("[ImportService] 警告：第" + (i+1) + "个题目解析失败");
                failCount++;
            }
        }
        
        System.out.println("[ImportService] 标准格式解析完成：成功" + successCount + "题，失败" + failCount + "题");
        return questions;
    }
    
    /**
     * 解析Android格式题库
     */
    private List<Question> parseAndroidFormat(JsonObject jsonObject) {
        List<Question> questions = new ArrayList<>();
        int totalCount = 0;
        
        // 解析单选题
        if (jsonObject.has("单选题")) {
            JsonObject singleChoice = jsonObject.getAsJsonObject("单选题");
            int count = parseQuestionCategory(singleChoice, "单选题", questions);
            totalCount += count;
            System.out.println("[ImportService] 单选题解析完成: " + count + "题");
        }
        
        // 解析多选题
        if (jsonObject.has("多选题")) {
            JsonObject multipleChoice = jsonObject.getAsJsonObject("多选题");
            int count = parseQuestionCategory(multipleChoice, "多选题", questions);
            totalCount += count;
            System.out.println("[ImportService] 多选题解析完成: " + count + "题");
        }
        
        // 解析判断题
        if (jsonObject.has("判断题")) {
            JsonObject trueFalse = jsonObject.getAsJsonObject("判断题");
            int count = parseQuestionCategory(trueFalse, "判断题", questions);
            totalCount += count;
            System.out.println("[ImportService] 判断题解析完成: " + count + "题");
        }
        
        System.out.println("[ImportService] Android格式解析完成：总计" + totalCount + "题");
        return questions;
    }
    
    /**
     * 解析Android格式中的某个题目类别
     */
    private int parseQuestionCategory(JsonObject categoryObj, String type, List<Question> questions) {
        int count = 0;
        for (String questionText : categoryObj.keySet()) {
            JsonObject questionData = categoryObj.getAsJsonObject(questionText);
            Question question = parseAndroidQuestion(questionText, questionData, type);
            if (question != null) {
                questions.add(question);
                count++;
            }
        }
        return count;
    }
    
    /**
     * 解析Android格式的单个题目
     */
    private Question parseAndroidQuestion(String questionText, JsonObject questionData, String type) {
        try {
            Question question = new Question();
            question.setId(UUID.randomUUID().toString());
            question.setQuestionText(questionText);
            question.setType(type);
            question.setCategory(type);
            
            // 解析答案
            if (questionData.has("answer")) {
                question.setAnswer(questionData.get("answer").getAsString());
            }
            
            // 解析选项
            List<String> options = new ArrayList<>();
            if (questionData.has("options")) {
                JsonArray optionsArray = questionData.getAsJsonArray("options");
                for (JsonElement option : optionsArray) {
                    options.add(option.getAsString());
                }
            }
            question.setOptions(options);
            
            // 解析解析（说明）
            if (questionData.has("explanation")) {
                question.setExplanation(questionData.get("explanation").getAsString());
            }
            
            return question;
        } catch (Exception e) {
            System.err.println("[ImportService] 解析Android题目失败: " + questionText + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析单个题目（标准格式）
     */
    private Question parseQuestion(JsonObject jsonObject) {
        try {
            Question question = new Question();
            
            // ID
            if (jsonObject.has("id")) {
                question.setId(jsonObject.get("id").getAsString());
            } else {
                question.setId(UUID.randomUUID().toString());
            }
            
            // 题目文本
            if (jsonObject.has("question") || jsonObject.has("questionText")) {
                String questionText = jsonObject.has("question") 
                    ? jsonObject.get("question").getAsString()
                    : jsonObject.get("questionText").getAsString();
                question.setQuestionText(questionText);
            }
            
            // 选项
            List<String> options = new ArrayList<>();
            if (jsonObject.has("options")) {
                JsonArray optionsArray = jsonObject.getAsJsonArray("options");
                for (JsonElement option : optionsArray) {
                    options.add(option.getAsString());
                }
            }
            question.setOptions(options);
            
            // 答案
            if (jsonObject.has("answer")) {
                question.setAnswer(jsonObject.get("answer").getAsString());
            }
            
            // 解析
            if (jsonObject.has("explanation")) {
                question.setExplanation(jsonObject.get("explanation").getAsString());
            }
            
            // 类别/类型
            if (jsonObject.has("category")) {
                question.setCategory(jsonObject.get("category").getAsString());
            }
            
            if (jsonObject.has("type")) {
                question.setType(jsonObject.get("type").getAsString());
            } else {
                // 根据category推断type
                String category = question.getCategory();
                if (category != null) {
                    if (category.contains("单选")) {
                        question.setType("单选题");
                    } else if (category.contains("多选")) {
                        question.setType("多选题");
                    } else if (category.contains("判断")) {
                        question.setType("判断题");
                    } else {
                        question.setType("单选题"); // 默认
                    }
                } else {
                    // 根据答案长度推断
                    String answer = question.getAnswer();
                    if (answer != null && answer.length() > 1 && !answer.contains("正确") && !answer.contains("错误")) {
                        question.setType("多选题");
                    } else if (answer != null && (answer.contains("正确") || answer.contains("错误"))) {
                        question.setType("判断题");
                    } else {
                        question.setType("单选题");
                    }
                }
            }
            
            return question;
        } catch (Exception e) {
            System.err.println("解析题目失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证题库文件格式
     */
    public boolean validateJsonFile(File file) {
        return validateJsonFileWithDetails(file) == null;
    }
    
    /**
     * 验证题库文件格式（返回详细错误信息）
     */
    public String validateJsonFileWithDetails(File file) {
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            System.out.println("[ImportService] 开始验证JSON文件...");
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            if (jsonObject == null) {
                return "无法解析JSON文件，请检查文件格式是否正确";
            }
            
            System.out.println("[ImportService] JSON根对象keys: " + jsonObject.keySet());
            
            // 检查是标准格式还是Android格式
            boolean hasQuestionsField = jsonObject.has("questions");
            boolean hasAndroidFormat = jsonObject.has("单选题") || jsonObject.has("多选题") || jsonObject.has("判断题");
            
            if (!hasQuestionsField && !hasAndroidFormat) {
                return "JSON文件格式不正确\n标准格式需要 'questions' 字段\nAndroid格式需要 '单选题'/'多选题'/'判断题' 字段\n当前字段: " + jsonObject.keySet();
            }
            
            // 验证标准格式
            if (hasQuestionsField) {
                System.out.println("[ImportService] 检测到标准格式");
                JsonArray questionsArray = jsonObject.getAsJsonArray("questions");
                if (questionsArray == null) {
                    return "'questions' 字段不是数组类型";
                }
                
                if (questionsArray.size() == 0) {
                    return "'questions' 数组为空，没有题目";
                }
                
                System.out.println("[ImportService] 题目数量: " + questionsArray.size());
                
                // 检查第一个题目的基本字段
                JsonElement firstElement = questionsArray.get(0);
                if (!firstElement.isJsonObject()) {
                    return "第1个题目不是对象类型";
                }
                
                JsonObject firstQuestion = firstElement.getAsJsonObject();
                System.out.println("[ImportService] 第1个题目的字段: " + firstQuestion.keySet());
                
                if (!firstQuestion.has("question") && !firstQuestion.has("questionText")) {
                    return "第1个题目缺少 'question' 或 'questionText' 字段\n当前字段: " + firstQuestion.keySet();
                }
            }
            
            // 验证Android格式
            if (hasAndroidFormat) {
                System.out.println("[ImportService] 检测到Android格式");
                int totalQuestions = 0;
                
                if (jsonObject.has("单选题")) {
                    JsonObject singleChoice = jsonObject.getAsJsonObject("单选题");
                    if (singleChoice != null) {
                        totalQuestions += singleChoice.keySet().size();
                        System.out.println("[ImportService] 单选题: " + singleChoice.keySet().size());
                    }
                }
                
                if (jsonObject.has("多选题")) {
                    JsonObject multipleChoice = jsonObject.getAsJsonObject("多选题");
                    if (multipleChoice != null) {
                        totalQuestions += multipleChoice.keySet().size();
                        System.out.println("[ImportService] 多选题: " + multipleChoice.keySet().size());
                    }
                }
                
                if (jsonObject.has("判断题")) {
                    JsonObject trueFalse = jsonObject.getAsJsonObject("判断题");
                    if (trueFalse != null) {
                        totalQuestions += trueFalse.keySet().size();
                        System.out.println("[ImportService] 判断题: " + trueFalse.keySet().size());
                    }
                }
                
                if (totalQuestions == 0) {
                    return "Android格式题库为空，没有题目";
                }
                
                System.out.println("[ImportService] Android格式题目总数: " + totalQuestions);
            }
            
            System.out.println("[ImportService] JSON验证通过");
            return null; // 验证通过
            
        } catch (Exception e) {
            System.err.println("[ImportService] 验证失败: " + e.getMessage());
            e.printStackTrace();
            return "文件读取或解析失败: " + e.getMessage();
        }
    }

    /**
     * 获取题库预览信息
     */
    public ImportPreview getPreview(File file) throws IOException {
        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            
            ImportPreview preview = new ImportPreview();
            
            // 题库名称
            String name = file.getName().replace(".json", "");
            if (jsonObject.has("name")) {
                name = jsonObject.get("name").getAsString();
            }
            preview.setName(name);
            
            // 检查格式
            boolean hasQuestionsField = jsonObject.has("questions");
            boolean hasAndroidFormat = jsonObject.has("单选题") || jsonObject.has("多选题") || jsonObject.has("判断题");
            
            if (hasQuestionsField) {
                // 标准格式
                JsonArray questionsArray = jsonObject.getAsJsonArray("questions");
                if (questionsArray != null) {
                    preview.setTotalQuestions(questionsArray.size());
                    
                    int singleChoice = 0;
                    int multipleChoice = 0;
                    int trueFalse = 0;
                    
                    for (JsonElement element : questionsArray) {
                        JsonObject questionObj = element.getAsJsonObject();
                        String type = getQuestionType(questionObj);
                        
                        switch (type) {
                            case "单选题":
                                singleChoice++;
                                break;
                            case "多选题":
                                multipleChoice++;
                                break;
                            case "判断题":
                                trueFalse++;
                                break;
                        }
                    }
                    
                    preview.setSingleChoiceCount(singleChoice);
                    preview.setMultipleChoiceCount(multipleChoice);
                    preview.setTrueFalseCount(trueFalse);
                }
            } else if (hasAndroidFormat) {
                // Android格式
                int singleChoice = 0;
                int multipleChoice = 0;
                int trueFalse = 0;
                
                if (jsonObject.has("单选题")) {
                    JsonObject obj = jsonObject.getAsJsonObject("单选题");
                    singleChoice = obj.keySet().size();
                }
                
                if (jsonObject.has("多选题")) {
                    JsonObject obj = jsonObject.getAsJsonObject("多选题");
                    multipleChoice = obj.keySet().size();
                }
                
                if (jsonObject.has("判断题")) {
                    JsonObject obj = jsonObject.getAsJsonObject("判断题");
                    trueFalse = obj.keySet().size();
                }
                
                preview.setTotalQuestions(singleChoice + multipleChoice + trueFalse);
                preview.setSingleChoiceCount(singleChoice);
                preview.setMultipleChoiceCount(multipleChoice);
                preview.setTrueFalseCount(trueFalse);
            }
            
            return preview;
        }
    }

    /**
     * 获取题目类型
     */
    private String getQuestionType(JsonObject questionObj) {
        if (questionObj.has("type")) {
            return questionObj.get("type").getAsString();
        }
        
        if (questionObj.has("category")) {
            String category = questionObj.get("category").getAsString();
            if (category.contains("单选")) return "单选题";
            if (category.contains("多选")) return "多选题";
            if (category.contains("判断")) return "判断题";
        }
        
        if (questionObj.has("answer")) {
            String answer = questionObj.get("answer").getAsString();
            if (answer.length() > 1 && !answer.contains("正确") && !answer.contains("错误")) {
                return "多选题";
            }
            if (answer.contains("正确") || answer.contains("错误")) {
                return "判断题";
            }
        }
        
        return "单选题";
    }

    /**
     * 导入预览信息类
     */
    public static class ImportPreview {
        private String name;
        private int totalQuestions;
        private int singleChoiceCount;
        private int multipleChoiceCount;
        private int trueFalseCount;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getTotalQuestions() {
            return totalQuestions;
        }

        public void setTotalQuestions(int totalQuestions) {
            this.totalQuestions = totalQuestions;
        }

        public int getSingleChoiceCount() {
            return singleChoiceCount;
        }

        public void setSingleChoiceCount(int singleChoiceCount) {
            this.singleChoiceCount = singleChoiceCount;
        }

        public int getMultipleChoiceCount() {
            return multipleChoiceCount;
        }

        public void setMultipleChoiceCount(int multipleChoiceCount) {
            this.multipleChoiceCount = multipleChoiceCount;
        }

        public int getTrueFalseCount() {
            return trueFalseCount;
        }

        public void setTrueFalseCount(int trueFalseCount) {
            this.trueFalseCount = trueFalseCount;
        }

        @Override
        public String toString() {
            return String.format("题库: %s\n总题数: %d\n单选题: %d\n多选题: %d\n判断题: %d",
                    name, totalQuestions, singleChoiceCount, multipleChoiceCount, trueFalseCount);
        }
    }
}