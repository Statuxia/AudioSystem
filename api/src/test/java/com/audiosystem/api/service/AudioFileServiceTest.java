package com.audiosystem.api.service;

import com.audiosystem.api.exception.JobCreationException;
import org.apache.tika.io.TikaInputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AudioFileServiceTest {

    public static final String DEFAULT_NAME = "DEFAULT_NAME";
    private final AudioFileService service = new AudioFileService();

    @Test
    @DisplayName("tests scenario with throwing IOException on getInputStream call")
    void testGetInputStreamInputStreamCausesException() throws IOException {
        final MultipartFile file = Mockito.mock(MultipartFile.class);
        BDDMockito.given(file.getInputStream()).willThrow(IOException.class);
        assertThrows(
            JobCreationException.class,
            () -> service.getInputStream(file),
            "failed to read input stream"
        );
    }

    @Test
    @DisplayName("tests scenario with throwing IOException on rewind enabling")
    @Disabled(value = "unreachable exception. offset in input stream doesn't cause exception")
    void testGetInputStreamRewindCausesException() throws IOException {
        final MultipartFile file = Mockito.mock(MultipartFile.class);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{3}, 1, 1);
        BDDMockito.given(file.getInputStream()).willReturn(inputStream);
        assertThrows(
            JobCreationException.class,
            () -> service.getInputStream(file),
            "failed to enable rewind for double-read stream"
        );
    }

    @Test
    @DisplayName("tests scenario with returned tika input stream")
    void testValidGetInputStream() throws IOException {
        final MultipartFile file = Mockito.mock(MultipartFile.class);
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{3});
        BDDMockito.given(file.getInputStream()).willReturn(inputStream);

        final TikaInputStream result = service.getInputStream(file);

        assertArrayEquals(new byte[]{3}, result.readAllBytes());
        assertDoesNotThrow(result::rewind);
        assertArrayEquals(new byte[]{3}, result.readAllBytes());
    }

    @Test
    void testInvalidAudioType() {
        assertFalse(service.isValidAudioType("no"));
    }

    @Test
    void testValidAudioType() {
        assertTrue(service.isValidAudioType("audio/mpeg"));
    }

    @Test
    void testGetContentTypeStreamCannotRead() throws IOException {
        final TikaInputStream inputStream
            = TikaInputStream.get(new ByteArrayInputStream(new byte[]{3}));
        inputStream.close();
        assertThrows(
            JobCreationException.class,
            () -> service.getContentType(inputStream),
            "failed to read input stream for content type"
        );
    }

    @Test
    @Disabled(value = "unreachable exception. rewind doesn't drop exception here")
    void testGetContentTypeRewindFailed() throws IOException {
        final TikaInputStream inputStream
            = TikaInputStream.get(new ClassPathResource("AudioFileServiceTest/" + "test2").getInputStream());
        assertThrows(
            JobCreationException.class,
            () -> service.getContentType(inputStream),
            "failed to rewind input stream"
        );
    }

    @ParameterizedTest
    @MethodSource("testValidGetContentType")
    void testValidGetContentType(String filename, String expectedContentType) throws IOException {
        final TikaInputStream inputStream = service.getInputStream(new MockMultipartFile(
            "/AudioFileServiceTest/" + filename,
            new ClassPathResource("AudioFileServiceTest/" + filename).getInputStream()
        ));

        final String contentType = service.getContentType(inputStream);
        assertEquals(expectedContentType, contentType);
    }

    @Test
    void testGetFileNameEmptyName() {

        final String name = service.getFileName(
            new MockMultipartFile(
                "test2",
                "",
                "audio/mpeg",
                new byte[0]
            ),
            DEFAULT_NAME
        );

        assertEquals("DEFAULT_NAME", name);
    }

    @Test
    void testGetFileNameOnlyFormat() {
        final String name = service.getFileName(
            new MockMultipartFile(
                "test2",
                ".mp3",
                "audio/mpeg",
                new byte[0]
            ),
            DEFAULT_NAME
        );

        assertEquals("DEFAULT_NAME", name);
    }

    @Test
    void testGetFileNameMultipleDots() {
        final String name = service.getFileName(
            new MockMultipartFile(
                "test2",
                "test.2.mp3",
                "audio/mpeg",
                new byte[0]
            ),
            DEFAULT_NAME
        );

        assertEquals("test.2", name);
    }

    @Test
    void testGetFileNameOnlyDirectory() {
        final String name = service.getFileName(
            new MockMultipartFile(
                "test2",
                "AudioFileServiceTest/",
                "audio/mpeg",
                new byte[0]
            ),
            DEFAULT_NAME
        );

        assertEquals("DEFAULT_NAME", name);
    }

    @Test
    void testGetFileNameFullName() throws IOException {
        final ClassPathResource resource = new ClassPathResource("AudioFileServiceTest/test2.mp3");

        final String name = service.getFileName(
            new MockMultipartFile(
                "test2",
                resource.getFile().toString(),
                "audio/mpeg",
                new byte[0]
            ),
            DEFAULT_NAME
        );

        assertEquals("test2", name);
    }

    @Test
    void testGetFileNameNoFormat() {
        final String name = service.getFileName(
            new MockMultipartFile(
                "test2",
                "test2",
                "audio/mpeg",
                new byte[0]
            ),
            DEFAULT_NAME
        );

        assertEquals("test2", name);
    }

    private static Stream<Arguments> testValidGetContentType() {
        return Stream.of(
            Arguments.of("test1", "image/webp"),
            Arguments.of("test1.webp", "image/webp"),
            Arguments.of("test1.mp3", "image/webp"),
            Arguments.of("test2", "audio/mpeg"),
            Arguments.of("test2.mp3", "audio/mpeg"),
            Arguments.of("test2.webp", "audio/mpeg")
        );
    }
}