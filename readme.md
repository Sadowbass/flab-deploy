# 무중단 배포

무중단 배포는 서비스의 중지 없이 새로운 버전, 오류 수정 등을 진행한 서비스를 운영환경에 배포하는 것이다.  

무중단 배포는 크게 3가지 방식이 존재한다.  
1. 롤링(Rolling)
```
롤링 방식은 운영 중인 서버를 전부 종료하고 새로 배포하지 않고 하나씩 하나씩 서버를 종료 후 재배포를 수행한다.
A 서버와 B 서버를 동시 종료하지 않고 A를 종료 후 새로운 버전을 배포 -> A의 배포가 완료된 후 B를 종료 후 배포한다.
장점은 구현이 가장 간단 가능하며 많은 관리 도구에서 지원하는 방식이다.
단점은 배포되는 과정에서 사용자 간 호환성의 문제가 생길 수 있으며 서버가 배포되는 과정에서 서버가 줄어들기 때문에 가용되는 서버들의 부하가 늘어나거나 부하가 늘어나는 것을 원치 않는다면 한 번에 배포하는 숫자만큼 서버 인스턴스를 늘려야 할 수 있다.
```

2. 블루/그린(Blue/Green)
```
블루/그린 방식은 새롭게 배포하고자 하는 서버의 인스턴스 수만큼 서버를 생성 후 로드밸런서에 연결하고 모두 연결되었을 때 기존에 운영하던 기존 서버를 모두 내려서 새롭게 배포한 서버로 로드밸런싱을 하는 방식이다.
장점은 일괄적으로 연결 및 제거가 되기 때문에 사용자 간 호환성 문제가 일어나지 않는다.
단점은 서버 자원이 2배로 필요하다.
```

3. 카나리(Canary)
```
롤링 방식과 흡사하지만 모든 서버를 배포하는 방식이 아닌 구버전과 신버전을 같이 운영하다 상태나 결과를 보고 전부 적용 혹은 롤백하는 방식이다.
특정 유저나 특정 서버의 접근만 신버전으로 돌리고 그 외에는 기존의 구버전으로 운영하다가 문제점이 없다고 판단되면 남은 구버전들도 새롭게 배포한다.
장점은 롤링과 마찬가지로 시스템 자원이 필요한 만큼만 소모되며 A/B 테스트가 용이하다. 운영 중 발생하는 문제에 대해 빠른 롤백이 가능하다.
단점 역시 롤링과 마찬가지로 사용자 간 호환성의 문제가 발생할 수 있다.
```

동적으로 로드밸런서에 서비스 혹은 서버를 추가하는 것은 Eureka 같은 서비스 디스커버리를 이용해야 한다.  
여기서는 단순하게 직접 로드밸런서를 구현하기 때문에 로드밸런서 실행 중 서버의 추가는 불가하며 처음에 등록한 서버가 죽었는지 살았는지에 따라 로드밸런싱 하는 기능만 존재한다.  
http api로 서버를 추가하는 방법 등으로 운영 중에도 서버의 추가가 가능하긴 하지만 오늘 중요한 건 무중단 배포이지 eureka의 제작이 아니기에 간단한 로드밸런서와 무중단 배포만 실습해 보았다.  

클라이언트라고 이름 지은 로드밸런싱 대상 서버는 현재 서버가 살아있는지를 응답하는 /ping과 간단한 api 하나만 구현하였다.
그리고 로드밸런서 역할을 하는 서버는 대상 서버의 정보를 yml에서 읽어 1초마다 ping을 날리고 3회 실패 시 사용가능한 큐에서 해당 서버를 제거한다.
물론 1초 뒤 다시 시도하여 성공하면 재등록이 가능하다.

여기서는 두개의 클라이언트를 껐다 켜면서 실습하기 위해 1초 단위로 핑을 확인하지만 eureka의 경우 하트비트(핑)을 30초 단위로 확인한다.  
두 개의 클라이언트를 각각 8081, 8082 포트로 실행하고 서버를 실행한다. (서버의 yml에는 기본적으로 대상 호스트가 http://127.0.0.1, 포트가 8081, 8082로 설정되어있다.)  
정상적으로 ping 요청을 주고받을 때 서버로 /api 요청하면 사용 가능한 서버 정보를 가져와 request하고 응답을 return 한다.  
그 상태에서 클라이언트를 하나 종료하면 핑 요청 3회 시도 후 최종 실패 시 큐에서 서버 정보를 삭제한다.  
종료한 클라이언트를 수정 후 재실행하면 서버가 자동으로 핑 요청을 하고 클라이언트가 응답 시 다시 사용 가능한 서버 큐에 정보를 등록한다.  

이 방식은 위에서 설명한 무중단 배포 방식 중 롤링에 해당하는 방식이다.  
로드밸런싱의 조건이 없이 라운드 로빈으로 연결되기에 카나리가 아니고 하나하나 서버를 종료하고 배포 후 로드밸런서에 연결되는 방식으로 블루/그린도 아니다.  

만들고 나서 생각해보니 유레카가 왜 http api 방식으로 서비스들을 관리하는지 알 것 같았다.  
일단 요즘 시대에 이렇게 정해진 ip(도메인)을 이용하는 경우는 거의 없다. k8s는 노드가 언제든 죽을 수 있는 소모품으로 사용하고 자동으로 배포할 때 매번 ip가 바뀌게 되어 새로 생긴 서버가 자신의 정보를 로드밸런서에게 알려주는 것이 합리적이고 확실하게 동작할 것이다.  
영원히 죽은, 없어진 서버에 1초마다 3회의 핑을 확인하는 것 자체가 당연히 동작하지 않을 것이기도 하고...  

오늘은 간단한 무중단 배포를 위한 로드 밸런서(리버스 프록시)의 구현을 해보았다.  
사실 이건 이전에 농협에서 API 게이트웨이를 만들 때 유레카를 사용하여 개발한 경험이 있어서 기억나는 대로 어느 정도 개발이 수월하게 가능하였다.  
발생할 수 있는 모든 예외를 잡아서 완벽하게 동작하는 로드밸런서를 만든 건 아니지만 그런 게 필요하면 유레카나 다른 서비스 디스커버리의 힘을 이용하지 직접 개발하면 하루가 아니라 수개월이 걸릴 것이기에 약식으로 개발하였음을 알린다.