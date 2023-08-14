
package com.ffmpeg.ffmpegtest.service;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class ffmpegCli {
    @Autowired
    private ResourceLoader resourceLoader;


    public void ffmpegCliTest(){
        try {
            String filePath = getFilePath();

            FFmpeg ffmpeg = new FFmpeg("/usr/bin/ffmpeg");
            FFprobe ffprobe = new FFprobe("/usr/bin/ffprobe");

            FFmpegProbeResult probeResult = ffprobe.probe(filePath);

            getMetaData(probeResult); // 해상도 출력


            FFmpegBuilder builder = new FFmpegBuilder()

                    .setInput(probeResult)     // Filename, or a FFmpegProbeResult
                    .overrideOutputFiles(true) // Override the output if it exists

                    .addOutput("/Users/jujinseong/mystudy/My_Treasure_Box/working_History/BackEnd_Spring/ffmpegTest/src/main/resources/result/output.mp4")   // Filename for the destination
                    .setFormat("mp4")        // Format is inferred from filename, or can be set
                    .setTargetSize(250_000)  // 출력 파일의 크기를 대략적으로 제어하는 데 사용되는 메서드 , 대상 크기에 가깝게 크기를 바꿈 ( 250KB 정도로 만들도록 지시 )

                    .disableSubtitle()       // No subtiles

                    .setAudioChannels(1)         // Mono audio
                    .setAudioCodec("aac")        // using the aac codec
                    .setAudioSampleRate(48_000)  // at 48KHz
                    .setAudioBitRate(32768)      // at 32 kbit/s

                    .setVideoCodec("libx264")     // Video using x264
                    .setVideoFrameRate(24, 1)     // at 24 frames per second
                    .setVideoResolution(640, 480) // at 640x480 resolution

                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL).done(); // Allow FFmpeg to use experimental specs

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);


// Run a one-pass encode
            executor.createJob(builder).run();

// Or run a two-pass encode (which is better quality at the cost of being slower)
            executor.createTwoPassJob(builder).run();

            log.info("success");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }


    // file input
    private String getFilePath() throws IOException {
        try {
            Resource resource = resourceLoader.getResource("classpath:testJinseong.mp4");
//            Resource resource = resourceLoader.getResource("classpath:testJinseongImage.jpeg");
            File file = resource.getFile();
            log.info("file path : "+file.getPath());
            return file.getPath();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 볼수있는 메타데이터 정보
     *
     * Format 정보:
     *      filename: 파일 이름
     *      nb_streams: 스트림의 개수
     *      nb_programs: 프로그램의 개수
     *      format_name: 포맷 이름 (예: "mov,mp4,m4a,3gp,3g2,mj2")
     *      format_long_name: 포맷의 긴 이름 (예: "QuickTime / MOV")
     *      start_time: 시작 시간 (초)
     *      duration: 지속 시간 (초)
     *      size: 파일 크기 (바이트)
     *      bit_rate: 비트레이트
     *      probe_score: 프로브 점수
     *      tags: 다양한 태그 (예: encoder, language 등)
     *
     * Streams 정보 (비디오, 오디오, 자막 등 각각의 스트림에 대한 정보):
     *      codec_name: 코덱 이름
     *      codec_long_name: 코덱의 긴 이름
     *      profile: 코덱 프로필
     *      codec_type: 스트림 타입 (예: "video", "audio")
     *      codec_time_base: 코덱의 타임베이스
     *      codec_tag: 코덱 태그
     *      width & height: 비디오 해상도 (비디오 스트림에만 해당)
     *      has_b_frames: B 프레임의 유무 (비디오 스트림에만 해당)
     *      sample_rate: 샘플 레이트 (오디오 스트림에만 해당)
     *      channels: 오디오 채널 수 (오디오 스트림에만 해당)
     *      channel_layout: 채널 레이아웃 (오디오 스트림에만 해당)
     *      bits_per_sample: 샘플 당 비트 수
     *      r_frame_rate: 프레임 레이트
     *      avg_frame_rate: 평균 프레임 레이트
     *      time_base: 스트림의 타임베이스
     *      start_pts & start_time: 스트림의 시작 시간 (PTS 및 초)
     *      duration_ts & duration: 스트림의 지속 시간 (PTS 및 초)
     *      bit_rate: 스트림의 비트레이트
     *      tags: 다양한 태그 (예: language, title 등)
     *
     * Chapters 정보:
     *      id: 챕터 ID
     *      time_base: 챕터의 타임베이스
     *      start & end: 챕터의 시작과 끝 (타임스탬프)
     *      tags: 챕터에 대한 태그
     */

    // 해상도 출력
    private void getMetaData(FFmpegProbeResult result){
        int width = result.getStreams().get(0).width; // 해상도 가로
        int height = result.getStreams().get(0).height; // 해상도 세로

        log.info("width : "+width);
        log.info("height : "+height);

        double durationInSeconds = result.getFormat().duration;// 영상 길이 ( 초단위 출력 )
        log.info("duration : "+durationInSeconds);

        String formatName = result.getFormat().format_name; // 영상 포멧 이름
        log.info("formatName : "+formatName);

        long size = result.getFormat().size;// 영상 사이즈 ( 바이트 단위 )
        log.info("size : "+size);
    }

}

