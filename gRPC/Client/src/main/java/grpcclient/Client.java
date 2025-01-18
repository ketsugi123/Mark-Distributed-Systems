package grpcclient;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLogger;
import io.grpc.netty.shaded.io.netty.util.internal.logging.InternalLoggerFactory;
import io.grpc.stub.StreamObserver;
import registryWithClient.GetSvcRequest;
import registryWithClient.GetSvcResponse;
import registryWithClient.RegistryWithClientGrpc;
import registryWithClient.RegistryWithClientGrpc.RegistryWithClientStub;
import shared.General.ServerInfo;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static grpcclient.DownloadHandler.download;
import static grpcclient.UploadHandler.upload;
import static io.grpc.ManagedChannelBuilder.forAddress;

public class Client {
	
	private static final InternalLogger logger = InternalLoggerFactory.getInstance(Client.class);
	private static final String[] ACTION_INFO = {"1 -> Upload a photo", "2 -> Download a marked photo", "99 -> Exit"};
	public static final String PATH_INFO = "Please provide the image path";
	public static final String IMAGE_INFO = "Please provide an image id";
	public static final String DOWNLOAD_INFO = "Please provide a path for your download";
	public static final long BLOCK_SIZE = 4096L * 1024 * 1024;
	private static final Scanner clientScanner = new Scanner(System.in);
	private static final IOHelper ioHelper = new IOHelper(clientScanner);
	public static AtomicBoolean shutdown = new AtomicBoolean(false);
	
	public static void main(String[] args) {
		String registryServerAddress;
		int registryServerPort;
		
		if (args.length >= 2) {
			registryServerPort = Integer.parseInt(args[0]);
			registryServerAddress = args[1];
		} else {
			logger.info("No Registry Server port and address specified");
			System.out.println("Usage: <REGISTRY_SERVER_ADDRESS> <REGISTRY_SERVER_PORT>");
			return;
		}
		
		ManagedChannel registryServerChannel = forAddress(registryServerAddress, registryServerPort)
				.usePlaintext()
				.build();
		
		RegistryWithClientStub registryServerClient =
				RegistryWithClientGrpc.newStub(registryServerChannel);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Shutdown hook triggered. Closing registryServerChannel...");
			try {
				registryServerChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
				logger.info("registryServerChannel closed successfully.");
			} catch (InterruptedException e) {
				logger.error("Error during registryServerChannel shutdown: " + e.getMessage());
				Thread.currentThread().interrupt();
			}
		}));

		try {
			Menu(registryServerClient);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void Menu(RegistryWithClientStub registryServerClient) throws IOException {
		while (true) {
			int option = ioHelper.readOption(ACTION_INFO);
			switch (option) {
				case 1: {
					String imagePathString = ioHelper.readString(PATH_INFO);

					logger.info("Requesting Service Server from Registry Server...");
					GetSvcRequest request = GetSvcRequest.newBuilder().build();

					registryServerClient.getSvc(request, new StreamObserver<>() {
						@Override
						public void onNext(GetSvcResponse response) {
							ServerInfo svcInfo = response.getSvc();
							String svcAddress = svcInfo.getIp();
							int svcPort = svcInfo.getPort();

							logger.info("Service Server Info: " + svcAddress + ":" + svcPort);
							try {
								upload(svcInfo, imagePathString);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						}

						@Override
						public void onError(Throwable t) {
							logger.error("An error occurred while contacting the Registry Server\nError: {}", t.getLocalizedMessage());
						}

						@Override
						public void onCompleted() {
							logger.info("Request to Registry Server completed");
						}
					});
					break;
				}
				case 2: {
					String imageId = ioHelper.readString(IMAGE_INFO);
					String downloadDir = ioHelper.readString(DOWNLOAD_INFO);

					logger.info("Requesting Service Server from Registry Server...");
					GetSvcRequest request = GetSvcRequest.newBuilder().build();

					registryServerClient.getSvc(request, new StreamObserver<>() {
						@Override
						public void onNext(GetSvcResponse response) {
							ServerInfo svcInfo = response.getSvc();
							String svcAddress = svcInfo.getIp();
							int svcPort = svcInfo.getPort();

							logger.info("Service Server Info: " + svcAddress + ":" + svcPort);
							download(svcInfo, imageId, downloadDir);
						}

						@Override
						public void onError(Throwable t) {
							logger.error("An error occurred while contacting the Registry Server\nError: {}", t.getLocalizedMessage());
						}

						@Override
						public void onCompleted() {
							logger.info("Request to Registry Server completed");
						}
					});
					break;
				}
				case 99:
					logger.info("Exiting application.");
					System.exit(0);
					break;
				default:
					logger.warn("Invalid menu option selected.");
			}
		}
	}
}