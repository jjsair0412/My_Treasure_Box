# 미디어 콘텐츠 고찰 with ffmpeg
미디어콘텐츠에 대해 일하면서 알게된 사실들을 정리한 문서

## 화질에 대하여
화질이 좋다는 해상도가 높다 가 아님.

해상도는 가로세로 길이에 대한것이지, 화질 자체에는 영향을 미치지 않음.

미디어 콘텐츠는 bit_rate 값이 화질에 민감하게 작용함.

비트레이트는 초당 전송되는 비트 수를 나타냄.

    일반적으로 비트레이트가 높을수록 미디어의 품질이 좋아지는데, 
    
    예를 들어 음악 파일의 비트레이트가 높으면 음질이 더 좋아지고, 동영상 파일의 비트레이트가 높으면 화질이 더 선명해집니다.

### ffmpeg를 통한 bit rate 조절
아래와 같은 방법으로 ffmpeg를 통해 python에서 bit rate를 조절할 수 있음.
```python
(
    ffmpeg
        .input('in.mp4')
        .output('out.mp4', **{'qscale:v': 3})
        .run()
)
```

## ffmpeg의 -crf Option
FFmpeg의 CRF(Constant Rate Factor)는 비디오 압축에서 사용되는 인코딩 매개변수입니다. 이것은 x264 및 x265과 같은 인코더에서 사용되며, 비디오의 품질을 설정하는 데 사용됨.

CRF는 최대값 51부터 최솟값 0까지의 값을 가질 수 있음.

    - stackExchange의 글 발췌

    0은 무손실, 23은 기본값, 51은 가장 안좋은 상태. 
    
    값이 낮을수록 품질이 높아지고 주관적으로 정상적인 범위는 18-28입니다. 
    
    18은 시각적으로 무손실 이거나 거의 그렇다고 생각합니다. 
    
    입력과 동일하거나 거의 동일해 보이지만 기술적으로 무손실은 아닙니다.

CRF는 특정 비디오 비트레이트를 명시적으로 설정하는 대신에 품질 수준을 지정. 

CRF 값이 낮을수록 (예: 0에 가까울수록) 높은 품질을 갖게되지만 파일 크기는 더 커짐. 반면에 CRF 값이 높을수록 (예: 51에 가까울수록) 낮은 품질이 되지만 파일 크기는 더 작아짐.

CRF는 대부분의 경우 추천되는 비디오 압축 방법. 

왜냐하면 특정 비트레이트를 선택하는 대신에, CRF를 사용하면 인코더가 비디오의 복잡성에 따라 동적으로 비트레이트를 조절하여 최적의 품질을 유지할 수 있기 때문.

- [crf_관련_stackOverflow](https://superuser.com/questions/677576/what-is-crf-used-for-in-ffmpeg)

### ffmpeg를 통한 crf 조절
아래와 같은 방법으로 ffmpeg를 통해 python에서 crf 값을 조절할 수 있음.
```python
(
    ffmpeg.input('in.mp4')\
        .output(
            'out.mp4',
            vcodec='libx264', 
            crf=40)\
        .run()
)
```

## 스트리밍에 대하여
기본적으로 .m3u8 파일과 .ts 파일로 스트리밍 파일을 생성함.

이는 HLS 방식이라 하며, .m3u8 파일과 .ts 세그먼트들은 object storage 내부에 저장되어야 하지만 , 같은 영상 데이터는 같은 경로에 위치하여야 함.


### HLS (HTTP Live Streaming) 라이브 스트리밍 방식
Http를 전송 채널로 하는 라이브 스트리밍 프로토콜

아래와 같은 구조를 갖고있기 때문에 , 라이브스트리밍이 가능하며 , 사용자 네트워크 대역폭에 유연히 대응할 수 있음.

### 구성 요소
.m3u8 타입의 파일과 , .ts 타입의 chunk로 나눠짐
- .ts 파일은 원본 파일을 특정 길이를 기준으로 (ex. 10초) 영상물을 분할시킨 chunk
- .m3u8 파일은 재생을 위해 .ts chunk들의 메타 정보를 모아놓고 있는 파일

### .m3u8 파일
m3u8 파일은 , master playlist , media playlist로 나뉘게 됨

***master playlist***
- 여러 미디어 플레이리스트의 참조 목록을 제공.
- 각 미디어 플레이리스트는 다른 해상도, 비트레이트 또는 다른 언어의 오디오 트랙과 같은 다른 특성을 가질 수 있음
- 클라이언트는 마스터 플레이리스트를 사용하여 사용 가능한 모든 스트림 중에서 최적의 스트림을 선택함.

- 예를 들어, 낮은 대역폭의 환경에서는 클라이언트가 더 낮은 비트레이트의 스트림을 선택할 수 있음.

master playlist 파일은 아래와 같은 구성을 가지고 있음.

```m3u8
#EXTM3U
#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
360p/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1600000,RESOLUTION=1280x720
720p/playlist.m3u8
```

***media playlist***
- 실제 미디어 세그먼트의 참조 목록을 제공
- 각 세그먼트는 일반적으로 짧은 시간 동안의 미디어 내용 (예: 2~10초)을 나타냄
- 미디어 플레이리스트는 #EXTINF 태그를 사용하여 각 세그먼트의 지속 시간을 지정함.

media playlist 파일은 아래와 같은 구성을 가지고 있음.

```m3u8
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:10
#EXT-X-MEDIA-SEQUENCE:0
#EXTINF:10,
segment0.ts
#EXTINF:10,
segment1.ts
```

### ffmpeg를 통한 HLS (HTTP Live Streaming) 라이브 스트리밍 방식 구현
아래와 같은 방법으로 ffmpeg를 통해 python에서 HLS (HTTP Live Streaming) 라이브 스트리밍 방식을 구현할 수 있음.

- outTs_%03d_.ts 로 ts 파일 이름을 선택한 이유는 , ts파일이 3초분량으로 생성되는데 in.mp4 파일이 3초 이상이라면 여러개 생성되기 때문에 순차적으로 만들어져야하기 때문.
- ```hls_list_size``` 옵션으로 HLS Playlist 파일에 포함되는 세그먼트 수를 지정함. 이를 0이 아닌 다른값을 두면. playlist 파일을 생성함.

```python
(
    ffmpeg.input('in.mp4')\
        .output(
            'out.m3u8',
            format='hls', 
            vcodec='libx264',
            hls_time=3,  # 3초
            hls_list_size=0, # hls_list_size=0를 설정하면서 HLS 스트리밍에서 재생목록 파일에 세그먼트를 포함하지 않게 됨. playlist파일을 만들지 않고 m3u8 , ts 파일만 생성
            hls_segment_filename='outTs_%03d_.ts'
        .run()
)
```