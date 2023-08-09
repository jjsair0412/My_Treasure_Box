# custom Exception 동작 방식
## 동작 방식
1. RuntimeException 등 Exception을 상속받는 custom Exception 발생
- customException은 생성자에서 error code , message , http status를 미리 정의해 둔 ErrorEnum class를 받아옴
- [customException](./customException.java)
- [errorEnum](./errorEnum.java)

2. 에러가 발생시, Eunm class를 생성자로 받음으로써 , 메시지 및 errorEnum class가 주입됨
- throw로 로직 수행중 에러 던짐
- [error testCode](./test.java)

3. 필터 체인이 모두 수행되고 난 뒤, 디스페쳐 서블릿이 호출되기 직전 , 발생한 customException을 @ExceptionHandler 가 받습니다.
- @RestControllerAdvice 클래스안에 @ExceptionHandler가 에러를 받습니다.
- ResponseEntity 를 반환하여 , json type으로 Error를 반환합니다.
- makeErrorResponse 메서드에서 , json type의 에러값을 생성합니다.
- [@RestControllerAdvice 코드](./errorAdvice.java)

## 결과
아래와 같은 json값으로 통일됩니다.

따라서 , [@RestControllerAdvice](./errorAdvice.java) 에서 어떤 Exception을 @ExceptionHandler 에서 받느냐에 따라서 달라집니다.
>사용 예로 default Exception들을 common하게 받고 , 에러 메세지가 명확해야할 경우에 custom Exception과 Enum에 에러코드 및 message를 생성해서 활용할 수 있습니다.  

```json
{
  "success": false,
  "error": {
    "path": "/test/path",
    "code": 404,
    "message": "error message"
  }
}
```

## ETC
### 1. 404 error 또한 처리하고싶을때 ..
404 에러는 기본적으로 RestControllerAdvice에서 처리되지 않습니다.

따라서 아래 설정을 따라야 합니다.

#### 1.1 application.properties 설정
```spring.mvc.throw-exception-if-no-handler-found=true``` 로 둠으로써 , 기본적으로 404 에러가 발생했을 경우 , WhitelabelErrorView 페이지가 출력되는 대신에
```NoHandlerFoundException``` 이 발생합니다.

```spring.web.resources.add-mappings=false``` 비활성화하여 , 정적 리소스(css, JavaScript 등) 에 대한 기본 매핑을 추가하지 않게끔 합니다.

```
# 404 error handler
spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false
```

#### 1.2 error 핸들링
```@ExceptionHandler```에 ```NoHandlerFoundException.class``` 예외를 받아줍니다.

```java

@ExceptionHandler({customException.class}) 
public ResponseEntity<?> bindException(HttpServletRequest request, Exception e) {
        return makeErrorResponse(HttpStatus.NOT_FOUND, request.getRequestURI(),e.getMessage());
}

private makeErrorResponse(HttpStatus status , String requestPath, String message) {
 // 에러 공통응답용 json 생성    
}
```