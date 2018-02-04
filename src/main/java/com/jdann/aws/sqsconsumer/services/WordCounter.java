package com.jdann.aws.sqsconsumer.services;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.jdann.aws.sqsconsumer.dao.WordTotalRepository;
import com.jdann.aws.sqsconsumer.dto.Word;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class WordCounter {

    private static final int LIMIT = 10;
    private static final String WORD_COUNT_QUEUE = "https://sqs.us-west-2.amazonaws.com/736338261372/word-count";
    private final WordTotalRepository wordTotalRepository;
    private final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();

    @Autowired
    public WordCounter(WordTotalRepository wordTotalRepository) {
        this.wordTotalRepository = wordTotalRepository;
    }

    @Scheduled(cron = "*/1 * * * * *")
    public void consume() {

        List<Message> messages = sqs.receiveMessage(WORD_COUNT_QUEUE).getMessages();

        if (!messages.isEmpty()) {
            Message message = messages.get(0);
            findTopWords(message.getBody());
            sqs.deleteMessage(WORD_COUNT_QUEUE, message.getReceiptHandle());
        }
    }

    private void findTopWords(String address) {

        if (address == null || address.trim().length() == 0) {
            return;
        }

        //check if already in database
        List<Word> words = wordTotalRepository.findAllWithHashKey(address);
        if (words.isEmpty()) {

            //need to process content for the first time
            String content = fetchContent(address);
            words = processContent(content, address);

            //save in database
            wordTotalRepository.save(words);
        }
    }

    private List<Word> processContent(String content, String address) {

        List<Word> words = new ArrayList<>();

        if (content == null) {
            return words;
        }

        //split the text by sequence of non-alphanumeric characters and get totals
        String[] wordSequence = content.split("[^\\w']");

        //get totals
        for (String word : wordSequence) {

            if (word.trim().length() == 0) {
                continue;
            }
            Word current = new Word(address, word);
            int i = words.indexOf(current);

            if (i > -1) {
                words.get(i).inc();
            } else {
                words.add(current);
            }
        }
        words.sort(Word.byTotal);

        //return entries up to the set limit
        return words.subList(0, words.size() > LIMIT ? LIMIT : words.size());
    }

    private String fetchContent(String address) {

        if (address == null) {
            return null;
        }

        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;

        try {
            url = new URL(address);
            is = url.openStream();
            br = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();

            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();

        } catch (IOException ignore) {
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignore) {
            }
        }
        return null;
    }
}