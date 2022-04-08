package pl.pal.kamil.pt6;

import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) throws IOException {

        int th = Integer.parseInt(args[2]);
        ForkJoinPool pool = new ForkJoinPool(th);

        long time = System.currentTimeMillis();

        List<Path> files;
        Path source = Path.of(args[0]);
        try (Stream<Path> stream = Files.list(source)) {

            files = stream.collect(Collectors.toList());

        } catch (IOException e) {
            System.err.println("Nie znaleziono ścieżki do katalogu");
            return;
        }

        Path output_dir = Path.of(args[1]);

        try {
            pool.submit(() -> {
                Stream<Path> col_stream = files.stream().parallel();
                Stream<ImmutablePair<Path, BufferedImage>> inputPairs = col_stream.map(Main::pathToImagePair);

                Stream<ImmutablePair<Path, BufferedImage>> outputImageOnly = inputPairs.map(Main::ModifyImage);

                Stream<ImmutablePair<Path, BufferedImage>> outputPairs = outputImageOnly.map(input -> {
                    Path out = Path.of(output_dir.toString(), input.left.getFileName().toString());
                    return new ImmutablePair<>(out, input.right);
                });


                outputPairs.forEach(input -> {
                    try {
                        ImageIO.write(input.right, "png", input.left.toFile());
                    } catch (IOException e) {
                        System.err.println("Nie zapisano do pliku.");
                    }
                });
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            return;
        }

        pool.shutdown();

        System.out.println((System.currentTimeMillis() - time) / 1000.0);
    }

    private static ImmutablePair<Path, BufferedImage> ModifyImage(ImmutablePair<Path, BufferedImage> input) {

        BufferedImage original = input.right;
        BufferedImage image = new BufferedImage(original.getWidth(),
                original.getHeight(),
                original.getType());

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int rgb = original.getRGB(x, y);

                Color color = new Color(rgb);
                int c = (int)(0.587*color.getGreen()+0.114*color.getBlue()+0.299*color.getRed());
                Color outColor = new Color(c,c,c);
                int outRgb = outColor.getRGB();
                image.setRGB(x, y, outRgb);

            }
        }
        return new ImmutablePair<>(input.left, image);
    }

    private static ImmutablePair<Path, BufferedImage> pathToImagePair(Path path) {
        try {
            return new ImmutablePair<>(path, ImageIO.read(path.toFile()));
        } catch (IOException e) {

            return new ImmutablePair<>(path, null);
        }
    }
}
