# make common response json
모든 json 반환에 대해서 , 공통처리하기위해 해당 폴더의 로직을 사용하였습니다.

## 기대 결과 type
모든 api에 대해 아래와 같은 json type을 내려주어야 합니다.
- 조건 1. 에러가 나거나 , 정상 처리되거나 구분 없이 Http status code는 200으로
- 조건 2. 아래와 같은 json 형태가 되어야 함.

```json
{
    "successResult": "true", // 에러발생시 false
    "status": "OK", // http status code
    "userInfo": { // response data
        "age": 27,
        "name": "jinseong"
    }
}
```

## 사용 코드
@RestController 어노테이션으로 controller 계층을 구성하고 , 각 api들은 모두 ResponseEntity 를 반환값으로 가집니다.
- ResponseEntity<> 의 데이터 타입은 , 반환되어야하는 dataEntity 객체가 되어야 함.
>해당 코드 가독성(테스트용이기 때문에)을 위해 서비스 메서드를 controller와 같이 작성해 두었음
  - [controller](./testController.java)
  - [데이터를 담고있는 entity 객체](./entity/userInfo.java)
  - [response시켜줄 Domain](./entity/sampleDomain.java)