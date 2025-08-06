Các bước chạy dự án bằng docker
mvn clean package -DskipTests
docker-compose up --build

cài đặt lại và dừng các container : 
docker-compose down -v
docker-compose up --build

Mỗi lần chạy lại dự án
docker-compose up

Thay đổi code trong Spring Boot và muốn build lại .jar
Thay đổi Dockerfile
docker-compose up --build


| Tình huống                       | Lệnh dùng                   |
| -------------------------------- | --------------------------- |
| 👉 Mở lại project và chạy        | `docker-compose up`         |
| 🔧 Sửa code hoặc Dockerfile      | `docker-compose up --build` |
| 🧹 Dọn sạch container (giữ data) | `docker-compose down`       |
| 💥 Dọn sạch tất cả, kể cả DB     | `docker-compose down -v`    |
