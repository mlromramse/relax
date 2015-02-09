import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by micke on 2015-02-08.
 */
public class AbstractTest {
    protected Logger log = LoggerFactory.getLogger(RelaxServerTest.class);

    @Rule
    public TestRule testRule = new TestRule() {
        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    log.debug("======================================================================");
                    log.debug(description.getDisplayName());
                    base.evaluate();
                }
            };
        }
    };


}
