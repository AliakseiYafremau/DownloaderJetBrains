# DownloaderJetBrains

## Task (verbatim)

Use either Java or Kotlin to implement a file downloader which has the ability to download chunks of a file in parallel. You should collect the parts from a web server by specifying a URL.
You may start a web server locally using the following docker command:

```bash
docker run --rm -p 8080:80 -v /path/to/your/local/directory:/usr/local/apache2/htdocs/ httpd:latest
```

Then you can access files from your local directory via localhost:8080.
For example: http://localhost:8080/my-local-file.txt

The web server is expected to behave like the following. When you send a HEAD request to the URL, the response will contain the headers :

```text
Accept-Ranges: bytes
Content-Length: <number of bytes>
```

To download a chunk of the file, you can send a GET request while specifying the Range header. For example:

```text
Range: bytes=1024-2047
```

The downloader should get chunks in parallel and combine them to a complete file.
Write unit tests to verify the correctness of your file downloader.

## How to Run the Project

Arguments format:

```text
downloader.MainKt <url> <targetPath> [minChunkSize] [maxChunkSize] [maxParallelDownloads]
```

### Example with default chunk and parallelism settings:
```bash
./gradlew run --args="http://localhost:8080/my-local-file.txt /tmp/my-local-file.txt"
```

```bash
./gradlew run --args="http://localhost:8080/file.bin /tmp/file.bin 1048576 8388608 4"
```

### How to Build and Use the Build

Build distributable scripts:

```bash
./gradlew installDist
```

Run the built application

```bash
./build/install/DownloaderJetBrains/bin/DownloaderJetBrains http://localhost:8080/my-local-file.txt /tmp/my-local-file.txt
```

Example with explicit chunk and parallelism settings:

```bash
./build/install/DownloaderJetBrains/bin/DownloaderJetBrains http://localhost:8080/file.bin /tmp/file.bin 1048576 8388608 4
```

## How to Run Tests

```bash
./gradlew test
```
