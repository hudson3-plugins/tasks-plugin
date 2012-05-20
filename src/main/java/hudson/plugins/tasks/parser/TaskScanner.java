package hudson.plugins.tasks.parser;


import hudson.AbortException;
import hudson.plugins.analysis.util.model.Priority;
import hudson.plugins.tasks.Messages;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;

/**
 * Scans a given input stream for open tasks.
 *
 * @author Ulli Hafner
 */
public class TaskScanner {
    private static final String WORD_BOUNDARY = "\\b";

    /** The regular expression patterns to be used to scan the files. One pattern per priority. */
    private final Map<Priority, Pattern> patterns = new HashMap<Priority, Pattern>();

    private boolean isInvalidPattern;
    private final StringBuilder errorMessage = new StringBuilder();

    /**
     * Creates a new instance of {@link TaskScanner}.
     */
    public TaskScanner() {
        this("FIXME", "TODO", "@deprecated", false);
    }

    /**
     * Creates a new instance of {@link TaskScanner}.
     *
     * @param high
     *            tag identifiers indicating high priority
     * @param normal
     *            tag identifiers indicating normal priority
     * @param low
     *            tag identifiers indicating low priority
     * @param ignoreCase
     *            if case should be ignored during matching
     */
    public TaskScanner(final String high, final String normal, final String low, final boolean ignoreCase) {
        if (StringUtils.isNotBlank(high)) {
            patterns.put(Priority.HIGH, compile(high, ignoreCase));
        }
        if (StringUtils.isNotBlank(normal)) {
            patterns.put(Priority.NORMAL, compile(normal, ignoreCase));
        }
        if (StringUtils.isNotBlank(low)) {
            patterns.put(Priority.LOW, compile(low, ignoreCase));
        }
    }

    /**
     * Compiles a regular expression pattern to scan for tag identifiers.
     *
     * @param tagIdentifiers
     *            the identifiers to scan for
     * @param ignoreCase
     *            specifies if case should be ignored
     * @return the compiled pattern
     */
    private Pattern compile(final String tagIdentifiers, final boolean ignoreCase) {
        try {
            String[] tags;
            if (tagIdentifiers.indexOf(',') == -1) {
                tags = new String[] {tagIdentifiers};
            }
            else {
                tags = StringUtils.split(tagIdentifiers, ",");
            }
            List<String> regexps = new ArrayList<String>();
            for (int i = 0; i < tags.length; i++) {
                String tag = tags[i].trim();
                if (StringUtils.isNotBlank(tag)) {
                    StringBuilder actual = new StringBuilder();
                    if (Character.isLetterOrDigit(tag.charAt(0))) {
                        actual.append(WORD_BOUNDARY);
                    }
                    actual.append(tag);
                    if (Character.isLetterOrDigit(tag.charAt(tag.length() - 1))) {
                        actual.append(WORD_BOUNDARY);
                    }
                    regexps.add(actual.toString());
                }
            }
            int flags;
            if (ignoreCase) {
                flags = Pattern.CASE_INSENSITIVE;
            }
            else {
                flags = 0;
            }
            return Pattern.compile("^.*(" + StringUtils.join(regexps.iterator(), "|") + ")(.*)$", flags);
        }
        catch (PatternSyntaxException exception) {
            isInvalidPattern = true;
            errorMessage.append(Messages.Tasks_PatternError(tagIdentifiers, exception.getMessage()));
            errorMessage.append("\n");

            return null;
        }
    }

    /**
     * Scans the specified input stream for open tasks.
     *
     * @param reader
     *            the file to scan
     * @return the result stored as java project
     * @throws IOException
     *             if we can't read the file
     */
    public Collection<Task> scan(final Reader reader) throws IOException {
        try {
            if (isInvalidPattern) {
                throw new AbortException(errorMessage.toString());
            }
            LineIterator lineIterator = IOUtils.lineIterator(reader);
            List<Task> tasks = new ArrayList<Task>();
            for (int lineNumber = 1; lineIterator.hasNext(); lineNumber++) {
                String line = (String)lineIterator.next();

                for (Priority priority : Priority.values()) {
                    if (patterns.containsKey(priority)) {
                        Matcher matcher = patterns.get(priority).matcher(line);
                        if (matcher.matches() && matcher.groupCount() == 2) {
                            String message = matcher.group(2).trim();
                            tasks.add(new Task(priority, lineNumber, matcher.group(1), StringUtils.remove(message, ":").trim()));
                        }
                    }
                }
            }

            return tasks;
        }
        finally {
            reader.close();
        }
    }
}

