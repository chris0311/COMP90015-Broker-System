/**
 * @Author: Chris Liang 1159696
 */

package error;

public class DuplicateRequestException extends Exception {
    public DuplicateRequestException(String message) {
        super(message);
    }
}
