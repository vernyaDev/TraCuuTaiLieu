# Ảnh của dịch vụ `app`. Giao diện KHÔNG có tiến trình riêng: Vite dựng ra tệp tĩnh ở tầng
# dưới đây, rồi máy chủ ứng dụng phục vụ chúng (kiến trúc Mục 1). Vì vậy một ảnh, một cổng,
# và không có phiên bản hoá giao diện lập trình — giao diện với máy chủ luôn cùng phiên bản.

# ── Tầng 1: dựng giao diện ─────────────────────────────────────────────────────────────────
FROM node:22-alpine AS web
WORKDIR /build

# Chép tệp khai phụ thuộc trước để tầng cài đặt được dùng lại khi chỉ mã nguồn đổi.
COPY web/package.json web/package-lock.json* ./
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

COPY web/ ./
# Bản dựng ghi thẳng vào thư mục tài nguyên tĩnh của máy chủ, nên đổi đích ở đây.
RUN npx vite build --outDir /build/static --emptyOutDir

# ── Tầng 2: dựng máy chủ ───────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS server
WORKDIR /build

COPY app/pom.xml app/.mvn/settings.xml ./

# Tham số chịu-lỗi-mạng. KHÔNG phải tối ưu hoá: qua một proxy doanh nghiệp, các artifact lớn
# (onnxruntime ~93 MB, tokenizers ~18 MB) bị cắt giữa chừng với "Could not transfer artifact",
# và Maven mặc định KHÔNG thử lại. Không có nhóm cờ này thì bản dựng hỏng ngẫu nhiên tuỳ hôm đó
# mạng thế nào — loại hỏng tệ nhất vì chạy lại đôi khi lại qua, nên dễ bị coi là "máy hôm nay dở".
ENV MVN_NET="-Dmaven.wagon.http.retryHandler.count=10              -Dmaven.wagon.http.retryHandler.requestSentEnabled=true              -Dmaven.wagon.httpconnectionManager.ttlSeconds=120              -Dmaven.wagon.http.pool=false              -Dhttp.keepAlive=false"

# Kho Maven để ở bộ nhớ đệm của BuildKit: bản dựng hỏng giữa chừng rồi chạy lại KHÔNG phải tải
# lại từ đầu. Với một tầng phụ thuộc vài trăm megabyte trên mạng hay đứt, đây là khác biệt giữa
# "thử lại mất một phút" và "thử lại mất mười phút".
#
# `|| true` là có chủ đích: mục tiêu go-offline hay báo lỗi vì vài plugin phụ mà bản dựng thật
# không cần. Nó chạy ở đây chỉ để LÀM ẤM bộ nhớ đệm; việc giải phụ thuộc thật do bước đóng gói
# bên dưới làm, và bước ĐÓ mới được phép quyết định bản dựng đúng hay sai.
RUN --mount=type=cache,target=/root/.m2/repository     mvn -s settings.xml -B $MVN_NET dependency:go-offline -q || true

COPY app/src ./src
COPY --from=web /build/static ./src/main/resources/static

# Chép jar ra ngoài bộ nhớ đệm: nội dung của cache mount KHÔNG nằm trong tầng ảnh, nên jar phải
# được đưa vào một đường dẫn thường trước khi tầng chạy chép nó.
# Chạy lại nhiều lần thay vì chạy một lần.
#
# Proxy doanh nghiệp cắt giữa chừng các tệp lớn — Maven báo "Premature end of Content-Length
# delimited message body". Cờ thử-lại bên trong Maven không đủ: nó thử lại từng yêu cầu, còn
# đây là một proxy cắt ĐỀU ĐẶN ở các tệp vài chục megabyte.
#
# Vòng lặp này hiệu quả vì mỗi lần chạy GIỮ LẠI những gì đã tải xong trong kho đệm: lần một
# tải được tám phần mười, lần hai tải nốt phần còn lại. Không có vòng lặp thì mỗi lần dựng là
# một lần tung đồng xu.
RUN --mount=type=cache,target=/root/.m2/repository     for lan in 1 2 3 4 5 6 7 8; do       echo "== dong goi, lan $lan ==" ;       mvn -s settings.xml -B $MVN_NET clean package -DskipTests -q && break ;       sleep 5 ;     done ;     test -f target/tracuu.jar || { echo "KHONG dong goi duoc sau 8 lan" ; exit 1 ; } ;     cp target/tracuu.jar /build/tracuu.jar

# ── Tầng 3: ảnh chạy ───────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# Chạy dưới tài khoản không đặc quyền: một lỗ hổng trong ứng dụng không nên đi kèm quyền quản
# trị trên cả máy chứa.
RUN groupadd --system tracuu && useradd --system --gid tracuu --home /app tracuu \
 && mkdir -p /var/tracuu/documents /var/tracuu/models \
 && chown -R tracuu:tracuu /var/tracuu

WORKDIR /app
COPY --from=server /build/tracuu.jar ./tracuu.jar
RUN chown -R tracuu:tracuu /app

USER tracuu

ENV JAVA_OPTS="-Xmx3g -XX:MaxRAMPercentage=75" \
    TRACUU_STORAGE_ROOT=/var/tracuu/documents \
    TRACUU_EMBEDDING_MODELPATH=/var/tracuu/models

EXPOSE 8080

# Dùng dạng shell để JAVA_OPTS được khai triển. Giữ tiến trình Java ở PID 1 bằng `exec` để
# tín hiệu dừng tới được nó — thiếu `exec` thì việc dừng có trật tự không bao giờ chạy, và
# một đợt nạp tài liệu đang dở sẽ bị cắt ngang.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/tracuu.jar"]
