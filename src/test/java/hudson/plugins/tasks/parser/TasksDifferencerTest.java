package hudson.plugins.tasks.parser;

import hudson.plugins.analysis.core.AnnotationDifferencer;
import hudson.plugins.analysis.test.AnnotationDifferencerTest;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.analysis.util.model.Priority;


/**
 * Tests the {@link AnnotationDifferencer} for tasks.
 */
public class TasksDifferencerTest extends AnnotationDifferencerTest {
    @Override
    public FileAnnotation createAnnotation(final String fileName, final Priority priority, final String message, final String category,
            final String type, final int start, final int end) {
        Task task = new Task(priority, start, type, message);
        task.setFileName(fileName);
        return task;
    }
}

