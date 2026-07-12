from PIL import Image, ImageChops, ImageGrab, ImageStat


class ScreenCapture:
    def __init__(self, max_width=1280, max_height=720):
        self.max_width = max_width
        self.max_height = max_height

    def capture(self):
        image = ImageGrab.grab(all_screens=True)
        return self.resize_for_vision(image)

    def resize_for_vision(self, image):
        image = image.convert("RGB")
        image.thumbnail(
            (self.max_width, self.max_height),
            Image.Resampling.LANCZOS,
        )
        return image

    def calculate_difference(self, previous_image, current_image):
        previous_sample = self._create_comparison_sample(previous_image)
        current_sample = self._create_comparison_sample(current_image)
        difference = ImageChops.difference(
            previous_sample,
            current_sample,
        )
        return float(ImageStat.Stat(difference).mean[0])

    def _create_comparison_sample(self, image):
        return image.convert("L").resize(
            (160, 90),
            Image.Resampling.BILINEAR,
        )
