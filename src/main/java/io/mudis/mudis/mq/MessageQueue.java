package io.mudis.mudis.mq;

import org.springframework.stereotype.Service;

import java.util.concurrent.SubmissionPublisher;

@Service
public class MessageQueue extends SubmissionPublisher<String> {
}
