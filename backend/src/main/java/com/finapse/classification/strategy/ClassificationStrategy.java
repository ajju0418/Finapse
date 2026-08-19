package com.finapse.classification.strategy;

import com.finapse.classification.model.ClassificationContext;
import com.finapse.classification.model.ClassificationResult;

public interface ClassificationStrategy {

    boolean supports(ClassificationContext context);

    ClassificationResult classify(ClassificationContext context);

    int getPriority();
}
