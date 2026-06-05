package com.socialogin.module.global.oauth.client;

import com.socialogin.module.global.exception.ErrorCode;
import com.socialogin.module.global.exception.OAuthLoginException;
import com.socialogin.module.global.oauth.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * provider에 맞는 OAuthClient를 찾아주는 클래스입니다.
 *
 * <p>SOLID 관점:
 * - OAuthService는 구체 클래스(GoogleOAuthClient 등)를 몰라도 됩니다.
 * - 새 provider가 추가되어도 Service는 수정하지 않고 Client 구현체만 추가합니다.
 */
@Component
public class OAuthClientFactory {
    // provider별 Client를 Map으로 들고 있다가 빠르게 찾아줍니다.
    private final Map<OAuthProvider, OAuthClient> clients;

    public OAuthClientFactory(List<OAuthClient> clients) {
        this.clients = createClientMap(clients);
    }

    public OAuthClient getClient(OAuthProvider provider) {
        // 요청 provider에 맞는 Client를 찾습니다.
        OAuthClient client = clients.get(provider);

        // Client가 없으면 Bean 등록이나 설정이 빠진 상황입니다.
        if (client == null) {
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED,
                    provider.getRegistrationId() + " OAuthClient가 등록되어 있지 않습니다."
            );
        }

        return client;
    }

    private Map<OAuthProvider, OAuthClient> createClientMap(List<OAuthClient> clients) {
        // enum key에 특화된 Map입니다.
        Map<OAuthProvider, OAuthClient> clientMap = new EnumMap<>(OAuthProvider.class);

        // Spring이 찾아준 모든 OAuthClient Bean을 provider별로 등록합니다.
        for (OAuthClient client : clients) {
            registerClient(clientMap, client);
        }

        // 외부에서 수정하지 못하게 불변 Map으로 바꿉니다.
        return Map.copyOf(clientMap);
    }

    private void registerClient(Map<OAuthProvider, OAuthClient> clientMap, OAuthClient client) {
        // Client가 담당하는 provider입니다.
        OAuthProvider provider = client.getProvider();

        // 같은 provider Client가 두 개 등록되면 어떤 것을 써야 할지 애매하므로 시작 단계에서 막습니다.
        if (clientMap.containsKey(provider)) {
            throw new OAuthLoginException(
                    ErrorCode.SOCIAL_PROVIDER_NOT_CONFIGURED,
                    provider.getRegistrationId() + " OAuthClient가 중복 등록되었습니다."
            );
        }

        clientMap.put(provider, client);
    }
}
