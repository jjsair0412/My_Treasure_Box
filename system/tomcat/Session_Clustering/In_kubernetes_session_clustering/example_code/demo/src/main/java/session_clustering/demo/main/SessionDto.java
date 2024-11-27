package session_clustering.demo.main;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Builder
@Setter
@Getter
public class SessionDto {

    private String sessionId;
    private boolean isNew;
    private Date sessionCreateTime;
    private Date sessionLastAccessTime;
    private String podName;
    private String podIp;
    private String namespace;

}


