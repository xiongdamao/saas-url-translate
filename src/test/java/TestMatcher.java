import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ke.xiong@oracle.com
 * @version 1.0
 * @date 2020-11-13 13:51
 */
public class TestMatcher {
    public static void main(String[] args) {
        String line = "https://efga-test.asdf2.ap4.oraclecloud.com/svcSrMgmt/adfasdf";
        String pattern = "efga-test.[a-zA-Z]+[0-9]*.ap4.oraclecloud.com/svcSrMgmt";

        // Create a Pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(line);
        if (m.find( )) {

            System.out.println("Found value: " + m.group(0) );
           System.out.println("Found value: " +   m.start() );
           System.out.println("Found value: " +  m.end() );
        }else {
            System.out.println("NO MATCH");
        }
    }
    }

