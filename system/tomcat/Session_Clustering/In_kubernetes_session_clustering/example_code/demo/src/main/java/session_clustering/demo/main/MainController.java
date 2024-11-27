package session_clustering.demo.main;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;

@Controller
@RequiredArgsConstructor
public class MainController {


    @Value("${MY_POD_NAME}")
    private String MY_POD_NAME;

    @Value("${MY_POD_NAMESPACE}")
    private String namespace;

    @Value("${MY_POD_IP}")
    private String MY_POD_IP;

    @GetMapping("/getSession")
    @ResponseBody
    public SessionDto getSession(HttpServletRequest request){
        HttpSession session = request.getSession();

        return SessionDto.builder()
                .sessionId(session.getId())
                .isNew(session.isNew())
                .sessionCreateTime(new Date(session.getCreationTime()))
                .sessionLastAccessTime(new Date(session.getLastAccessedTime()))
                .podName(MY_POD_NAME)
                .podIp(MY_POD_IP)
                .namespace(namespace).build();

    }
}
