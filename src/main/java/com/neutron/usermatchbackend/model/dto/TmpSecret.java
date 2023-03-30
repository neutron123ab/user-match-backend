package com.neutron.usermatchbackend.model.dto;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @author zzs
 * @date 2023/3/30 20:14
 */
@Data
@Component
public class TmpSecret {

    private String secretId;

    private String secretKey;

    private String sessionToken;

}
