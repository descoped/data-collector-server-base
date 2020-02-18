package no.ssb.dc.application.engine;

import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FreemarkerTemplateTest {

    public static final String FREEMARKER_TEMPLATES = "/freemarker-templates";

    @Test
    void renderSimpleModel() throws IOException, TemplateException {
        FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(FREEMARKER_TEMPLATES);

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("user", "Big Joe");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        engine.process("single-model-html.ftl", dataModel, pw);

        assertTrue(sw.toString().contains("Big Joe"), "data model NOT rendered by template engine");
    }

    @Test
    void renderListModel() {
        FreemarkerTemplateEngine engine = new FreemarkerTemplateEngine(FREEMARKER_TEMPLATES);

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("user", "Big Joe");
        List<EventItem> eventList = new ArrayList<>();
        for (int n = 1; n < 2; n++) {
            for (int m = 97; m < 97 + 10; m++) {
                eventList.add(new EventItem(String.valueOf(n), Character.toString(m)));
            }
        }
        dataModel.put("eventList", eventList);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        engine.process("list-model-html.ftl", dataModel, pw);

        String rendered = sw.toString();
        assertTrue(rendered.contains("<span>1</span>"), "data model NOT rendered by template engine");
        assertTrue(rendered.contains("<span>a</span>"), "data model NOT rendered by template engine");
        assertTrue(rendered.contains("<span>1</span>"), "data model NOT rendered by template engine");
        assertTrue(rendered.contains("<span>b</span>"), "data model NOT rendered by template engine");
    }

    public class EventItem {
        private final String id;
        private final String eventId;

        public EventItem(String id, String eventId) {
            this.id = id;
            this.eventId = eventId;
        }

        public String getId() {
            return id;
        }

        public String getEventId() {
            return eventId;
        }
    }
}
