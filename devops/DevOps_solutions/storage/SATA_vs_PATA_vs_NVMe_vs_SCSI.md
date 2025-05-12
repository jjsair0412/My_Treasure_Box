# SATA vs PATA vs NVMe vs SCSI
## Overview
스토리지 연결방식의 차이점에 대해 비교한 글
- [해당_문서_번역](https://www.pitsdatarecovery.co.uk/blog/sata-vs-pata-vs-nvme-vs-scsi/)
- [참고_SATA_vs_NVMe](./SATA_VS_NVMe.md)

## 1. PATA(Parallel ATA) or IDE
- PATA 또는 IDE(Integrated Drive Electronics)라 부름
- 사용 시기 : 1986년 등장 ~ 2000년대까지 사용
- 전송 방식: 병렬 데이터 전송 (여러 비트를 동시에 전송)
- 케이블: 40핀 또는 80핀의 넓고 두꺼운 리본 케이블
- 속도: 최대 133MB/s

### 1.1 단점
- 속도 느림
- 케이블 굵기가 두꺼워 공간 많이 차지함

## 2. SATA(Serial ATA)
- 출시 시기: 2003년
- 전송 방식: 직렬 데이터 전송
- 속도
    - SATA 1.0: 1.5 Gbps (약 150MB/s)
    - SATA 2.0: 3.0 Gbps (약 300MB/s)
    - SATA 3.0: 6.0 Gbps (약 600MB/s)

### 2.1 장점
- PATA에 비해 성능 향상
- 케이블 정리가 용이함.

### 2.2 단점
- 단일 명령 큐를 사용하여, 이전 명령이 처리되는 동안 다른 명령이 대기해야 함. HDD에는 적합하나 고속 SSD에서는 병목 현상 발생


## 3. NVMe
- 출시 시기: 2011년
- 전송 방식: PCIe(Peripheral Component Interconnect Express) 인터페이스를 통한 직렬 전송
- 데이터 전송 속도: PCIe 버전에 따라 다름
    - PCIe 3.0 x4: 약 3,500MB/s
    - PCIe 4.0 x4: 약 7,000MB/s
    - PCIe 5.0 x4: 약 14,000MB/s

### 3.1 장점
- SSD 성능 극대화
- 낮은 지연시간, 높은 처리량
- CPU 직접 연결로 빠른 데이터 전송

### 3.2 단점
- 높은 비용

## 4. SCSI(Small Computer System Interface)
- 출시 시기: 1970년대 중반
- 전송 방식: 초기에는 병렬, 이후 직렬로 발전됨.
- 속도
    - 최대 80MB/s (초기 버전 기준)
- 용도
    - 스캐너, 프린터, 하드 디스크 등 다양한 장치 연결
    - 여러 장치를 하나의 버스에 연결 가능
