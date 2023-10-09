package com.example.mangoplatebackup;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MangoplateBackupRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (! args.containsOption("backupFile")) {
            throw new RuntimeException("input mangoplate backup csv file");
        }

        String backupFilePath = args.getOptionValues("backupFile").stream().findFirst().get();

        // CSV 파일 파싱
        FileReader fileReader = new FileReader(backupFilePath);
        CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(fileReader);

        // CSV 행 반복
        for (CSVRecord csvRecord : csvParser) {
            String restaurantName = csvRecord.get("식당이름");
            String restaurantBranch = csvRecord.get("식당지점이름");
            String restaurantAddress = csvRecord.get("식당주소");
            String restaurantRating = csvRecord.get("식당평점");
            String reviewRating = csvRecord.get("리뷰평점");
            String reviewText = csvRecord.get("리뷰본문");
            String reviewTimestamp = csvRecord.get("리뷰작성일시");

            // 도, 시 까지 디렉터리 만들기
            String[] splitRestaurantAddress = StringUtils.split(restaurantAddress);
            String restaurantAddressCity = splitRestaurantAddress[0] + " " + splitRestaurantAddress[1];
            Path restaurantAddressCityDir = Path.of(backupFilePath).resolveSibling(restaurantAddressCity);
            if (! Files.exists(restaurantAddressCityDir)) {
                Files.createDirectories(restaurantAddressCityDir);
            }

            // 식당 디렉터리 생성
            DateTimeFormatter parseFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);
            DateTimeFormatter simpleFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            ZonedDateTime createDateUtc = ZonedDateTime.parse(reviewTimestamp, parseFormatter);
            ZonedDateTime createDateKst = createDateUtc.withZoneSameInstant(ZoneId.of("Asia/Seoul"));

            Path directoryPath = restaurantAddressCityDir.resolve(restaurantName + "_" + StringUtils.remove(reviewRating, "!") +
                    "_" + createDateKst.format(simpleFormatter));

            if (Files.exists(directoryPath)) {
                System.out.println("already exists directory. " + directoryPath);
                continue;
            }

            Files.createDirectories(directoryPath);

            // reviews.txt 파일 생성 및 내용 작성
            String reviewsFileName = restaurantName + "_reviews.txt";
            Path reviewsFilePath = directoryPath.resolve(reviewsFileName);
            String reviewsContent = "식당이름: " + restaurantName + "\n"
                    + "식당지점이름: " + restaurantBranch + "\n"
                    + "리뷰작성일시: " + createDateKst + "\n"
                    + "식당주소: " + restaurantAddress + "\n"
                    + "식당평점: " + restaurantRating + "\n"
                    + "리뷰평점: " + reviewRating + "\n"
                    + "리뷰본문:\n" + reviewText + "\n";
            Files.write(reviewsFilePath, reviewsContent.getBytes(), StandardOpenOption.CREATE);

            System.out.println("restaurantName: " + restaurantName);
            // 이미지 다운로드 및 저장
            for (int i = 1; i <= 30; i++) {
                if (! csvRecord.isSet("사진" + i)) {
                    break;
                }

                String imageUrl = csvRecord.get("사진" + i);
                if (!imageUrl.isEmpty()) {
                    String imageFileName = restaurantName + "_image_" + i + ".jpg";
                    Path imagePath = directoryPath.resolve(imageFileName);
                    System.out.println("imageUrl: " + imageUrl + ", imagePath: " + imagePath);
                    downloadImage(imageUrl, imagePath.toString());
                }
            }
        }

        // 리소스 정리
        csvParser.close();
        fileReader.close();

        System.out.println("CSV 파일 파싱 및 디렉터리 및 파일 생성 완료.");
    }

    private static void downloadImage(String imageUrl, String destinationPath) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(imageUrl);
            HttpResponse httpResponse = httpClient.execute(httpGet);

            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                try (InputStream imageStream = httpResponse.getEntity().getContent()) {
                    Files.copy(imageStream, Path.of(destinationPath));
                }
            }
        }
    }
}
