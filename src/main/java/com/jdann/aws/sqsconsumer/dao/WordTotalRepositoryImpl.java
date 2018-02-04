package com.jdann.aws.sqsconsumer.dao;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.TableCollection;
import com.amazonaws.services.dynamodbv2.model.*;
import com.jdann.aws.sqsconsumer.dto.Word;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WordTotalRepositoryImpl implements WordTotalRepository {

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static DynamoDBMapper mapper = new DynamoDBMapper(client);
    private static DynamoDB dynamoDB = new DynamoDB(client);

    public static void main(String[] args) {
        WordTotalRepositoryImpl wordTotalRepository = new WordTotalRepositoryImpl();

        List<Word> words = wordTotalRepository.findAllWithHashKey(null);
        System.out.println(words);
    }

    @Override
    public void save(Word word) {
        mapper.save(word);
    }

    @Override
    public void save(List<Word> words) {
        words.forEach(this::save);
    }

    @Override
    public Word find(String url, String word) {
        return mapper.load(Word.class, url, word);
    }

    @Override
    public void delete(Word word) {
        mapper.delete(word);
    }

    @Override
    public List<Word> findAllWithHashKey(String hashKey) {

        Map<String, AttributeValue> attributeValues = new HashMap<>();
        attributeValues.put(":val", new AttributeValue().withS(hashKey));

        ScanRequest scanRequest = new ScanRequest()
                .withTableName(Word.TABLE_NAME)
                .withFilterExpression(Word.ADDRESS + " = :val")
                .withExpressionAttributeValues(attributeValues);

        ScanResult result = client.scan(scanRequest);
        List<Word> words = new ArrayList<>();

        //marshall the results into WordTotals
        result.getItems().forEach(item -> {

            Word word = new Word();
            item.forEach((k, v) -> {
                switch (k) {
                    case Word.ADDRESS   : word.setAddress(v.getS()); break;
                    case Word.WORD      : word.setWord(v.getS()); break;
                    case Word.TOTAL     : word.setTotal(Long.valueOf(v.getN())); break;
                }
            });
            words.add(word);
        });
        return words;
    }

    @Override
    public boolean createTableIfNotExist() {

        //check if table exists
        TableCollection<ListTablesResult> tables = dynamoDB.listTables();

        for (Table table : tables) {
            if (table.getTableName().equals(Word.TABLE_NAME)) {
                return true;
            }
        }

        //create table
        CreateTableRequest request = mapper.generateCreateTableRequest(Word.class);
        request.setProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

        try {
            client.createTable(request);
            return true;
        } catch (ResourceInUseException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteTable(String tableName) {

        Table table = dynamoDB.getTable(tableName);
        table.delete();
        try {
            table.waitForDelete();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
