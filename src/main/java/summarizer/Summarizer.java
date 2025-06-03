package summarizer;

import java.io.IOException;

public interface Summarizer {
    String summarize(String text, SummaryType type) throws Exception, IOException;
}   

